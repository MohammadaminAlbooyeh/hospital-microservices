from __future__ import annotations

import logging
from datetime import datetime, timezone
from decimal import Decimal
from typing import Any, Callable

from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.core.config import settings
from app.domain.adjudication import ClaimStatus, auto_adjudicate, generate_claim_number, round_money
from app.domain.eligibility import check_eligibility
from app.infrastructure.db import Claim, Policy, SessionLocal
from hospital_shared.events.models import InvoiceIssued, PaymentCompleted, parse_event
from hospital_shared.kafka import EventConsumer

logger = logging.getLogger(__name__)

SessionFactory = Callable[[], AsyncSession]


async def _handle_invoice_issued(
    payload: InvoiceIssued,
    session_factory: Callable[[], AsyncSession],
) -> None:
    invoice_id = str(payload.invoice_id)
    now = datetime.now(timezone.utc)

    async with session_factory() as session:
        existing = await session.scalar(select(Claim.id).where(Claim.invoice_id == invoice_id))
        if existing is not None:
            logger.info("Claim for invoice %s already exists; skipping", invoice_id)
            return

        policy = await session.scalar(
            select(Policy)
            .where(Policy.patient_id == payload.patient_id)
            .order_by(Policy.active.desc(), Policy.valid_to.desc())
        )

        if policy is None:
            eligibility = check_eligibility(None, "")
            policy_number = "UNKNOWN"
        else:
            eligibility = check_eligibility(policy, policy.member_id, now=now)
            policy_number = policy.policy_number

        amount = round_money(Decimal(str(payload.total_amount)))
        outcome = auto_adjudicate(amount, eligibility)
        claim = Claim(
            claim_number=generate_claim_number(now),
            policy_number=policy_number,
            patient_id=payload.patient_id,
            encounter_id=payload.encounter_id,
            invoice_id=invoice_id,
            amount=amount,
            status=outcome.status,
            approved_amount=outcome.approved_amount,
            rejection_reason=outcome.rejection_reason,
            submitted_at=now,
            updated_at=now,
        )
        session.add(claim)
        try:
            await session.commit()
        except IntegrityError:
            await session.rollback()
            logger.info("Concurrent claim creation for invoice %s; ignoring duplicate", invoice_id)
            return

    logger.info("Created claim for invoice %s", invoice_id)


async def _handle_payment_completed(
    payload: PaymentCompleted,
    session_factory: Callable[[], AsyncSession],
) -> None:
    invoice_id = str(payload.invoice_id)
    now = datetime.now(timezone.utc)

    async with session_factory() as session:
        claims = (
            await session.scalars(select(Claim).where(Claim.invoice_id == invoice_id))
        ).all()
        if not claims:
            logger.info("No claims found for paid invoice %s", invoice_id)
            return
        for claim in claims:
            claim.status = ClaimStatus.PAID.value
            claim.updated_at = now
        await session.commit()

    logger.info("Marked %d claim(s) paid for invoice %s", len(claims), invoice_id)


async def handle_event(
    event: dict[str, Any],
    *,
    session_factory: Callable[[], AsyncSession] = SessionLocal,
) -> None:
    try:
        envelope, payload = parse_event(event)
    except ValidationError as exc:
        logger.warning("Discarding invalid event: %s", exc)
        return

    if envelope.event_type == "InvoiceIssued" and isinstance(payload, InvoiceIssued):
        await _handle_invoice_issued(payload, session_factory)
    elif envelope.event_type == "PaymentCompleted" and isinstance(payload, PaymentCompleted):
        await _handle_payment_completed(payload, session_factory)
    else:
        logger.debug("Insurance service ignoring event %s", envelope.event_type)


async def _consume(event: dict[str, Any]) -> None:
    await handle_event(event)


def build_consumer() -> EventConsumer:
    return EventConsumer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        topics=[settings.kafka_topic],
        handler=_consume,
        client_id=settings.service_name,
    )
