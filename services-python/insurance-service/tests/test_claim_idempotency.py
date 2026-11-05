from __future__ import annotations

from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.infrastructure.db import Base, Claim, Policy
from app.infrastructure.kafka_consumer import handle_event

NOW = datetime.now(timezone.utc)


@pytest.fixture
async def session_factory():
    engine = create_async_engine(
        "sqlite+aiosqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    maker = async_sessionmaker(engine, expire_on_commit=False)
    try:
        yield maker
    finally:
        await engine.dispose()


async def _add_policy(session_factory, *, coverage_percent: int = 80) -> None:
    async with session_factory() as session:
        session.add(
            Policy(
                policy_number="POL-1",
                provider="Acme Health",
                member_id="MEM-1",
                patient_id="PAT-1",
                coverage_percent=coverage_percent,
                valid_from=NOW - timedelta(days=30),
                valid_to=NOW + timedelta(days=30),
                active=True,
            )
        )
        await session.commit()


def invoice_event(event_id: str, invoice_id: str = "INV-1") -> dict:
    return {
        "event_id": event_id,
        "event_type": "InvoiceIssued",
        "occurred_at": NOW.isoformat(),
        "aggregate_id": invoice_id,
        "payload": {
            "invoiceId": invoice_id,
            "encounterId": "ENC-1",
            "patientId": "PAT-1",
            "totalAmount": 200.0,
            "patientShare": 40.0,
            "insurerShare": 160.0,
            "currency": "USD",
        },
    }


async def test_invoice_event_creates_claim_once(session_factory):
    await _add_policy(session_factory)

    await handle_event(invoice_event("EVT-1"), session_factory=session_factory)
    await handle_event(invoice_event("EVT-2"), session_factory=session_factory)

    async with session_factory() as session:
        count = await session.scalar(select(func.count()).select_from(Claim))
        claim = await session.scalar(select(Claim).where(Claim.invoice_id == "INV-1"))

    assert count == 1
    assert claim is not None
    assert claim.status == "APPROVED"
    assert claim.approved_amount == Decimal("160.00")
    assert claim.amount == Decimal("200.00")
    assert claim.encounter_id == "ENC-1"


async def test_invoice_event_rejects_when_policy_missing(session_factory):
    await handle_event(invoice_event("EVT-3"), session_factory=session_factory)

    async with session_factory() as session:
        claim = await session.scalar(select(Claim).where(Claim.invoice_id == "INV-1"))

    assert claim is not None
    assert claim.status == "REJECTED"
    assert claim.approved_amount is None
    assert claim.policy_number == "UNKNOWN"
    assert "not found" in claim.rejection_reason


async def test_payment_completed_marks_claim_paid(session_factory):
    await _add_policy(session_factory)
    await handle_event(invoice_event("EVT-4"), session_factory=session_factory)

    payment_event = {
        "event_id": "EVT-5",
        "event_type": "PaymentCompleted",
        "occurred_at": NOW.isoformat(),
        "aggregate_id": "PAY-1",
        "payload": {
            "paymentId": "PAY-1",
            "invoiceId": "INV-1",
            "patientId": "PAT-1",
            "amount": 160.0,
            "currency": "USD",
        },
    }
    await handle_event(payment_event, session_factory=session_factory)
    await handle_event(payment_event, session_factory=session_factory)

    async with session_factory() as session:
        claim = await session.scalar(select(Claim).where(Claim.invoice_id == "INV-1"))

    assert claim is not None
    assert claim.status == "PAID"


async def test_invalid_event_is_ignored(session_factory):
    await handle_event({"event_type": "InvoiceIssued"}, session_factory=session_factory)

    async with session_factory() as session:
        count = await session.scalar(select(func.count()).select_from(Claim))

    assert count == 0
