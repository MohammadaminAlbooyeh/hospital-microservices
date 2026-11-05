from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import select

from app.core.config import settings
from app.infrastructure.db import AppointmentFact, EncounterFact, InvoiceFact, SessionLocal
from hospital_shared.events import (
    AppointmentBooked,
    AppointmentCancelled,
    EncounterCompleted,
    InvoiceIssued,
    PaymentCompleted,
    parse_event,
)
from hospital_shared.kafka import EventConsumer

logger = logging.getLogger(__name__)


def _extra(payload: Any, name: str, default: Any = None) -> Any:
    value = getattr(payload, name, None)
    if value is not None:
        return value
    extras = getattr(payload, "model_extra", None) or {}
    return extras.get(name, default)


def _moment(value: datetime | None, fallback: datetime) -> datetime:
    moment = value or fallback
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=timezone.utc)
    return moment


async def _handle_appointment_booked(envelope, payload: AppointmentBooked) -> None:
    async with SessionLocal() as session:
        existing_event = await session.scalar(
            select(AppointmentFact).where(AppointmentFact.event_id == envelope.event_id)
        )
        if existing_event is not None:
            return

        booked_at = _moment(payload.booked_at, envelope.occurred_at)
        existing = (
            await session.execute(
                select(AppointmentFact).where(AppointmentFact.appointment_id == payload.appointment_id)
            )
        ).scalars().first()
        if existing is not None:
            existing.patient_id = payload.patient_id
            existing.doctor_id = payload.doctor_id
            existing.department_id = payload.department_id
            existing.slot_start = _moment(payload.slot_start, envelope.occurred_at)
            existing.booked_at = booked_at
            if existing.status != "CANCELLED":
                existing.status = "BOOKED"
        else:
            session.add(
                AppointmentFact(
                    event_id=envelope.event_id,
                    appointment_id=payload.appointment_id,
                    patient_id=payload.patient_id,
                    doctor_id=payload.doctor_id,
                    department_id=payload.department_id,
                    slot_start=_moment(payload.slot_start, envelope.occurred_at),
                    booked_at=booked_at,
                    status="BOOKED",
                )
            )
        await session.commit()


async def _handle_appointment_cancelled(envelope, payload: AppointmentCancelled) -> None:
    async with SessionLocal() as session:
        facts = (
            await session.execute(
                select(AppointmentFact).where(AppointmentFact.appointment_id == payload.appointment_id)
            )
        ).scalars().all()
        cancelled_at = _moment(payload.cancelled_at, envelope.occurred_at)
        if facts:
            for fact in facts:
                fact.status = "CANCELLED"
        else:
            session.add(
                AppointmentFact(
                    event_id=envelope.event_id,
                    appointment_id=payload.appointment_id,
                    patient_id=payload.patient_id,
                    doctor_id=str(_extra(payload, "doctor_id", "UNKNOWN")),
                    department_id=_extra(payload, "department_id"),
                    slot_start=cancelled_at,
                    booked_at=cancelled_at,
                    status="CANCELLED",
                )
            )
        await session.commit()


async def _handle_encounter_completed(envelope, payload: EncounterCompleted) -> None:
    async with SessionLocal() as session:
        exists = await session.scalar(
            select(EncounterFact.id).where(EncounterFact.event_id == envelope.event_id)
        )
        if exists is not None:
            return
        completed_at = _moment(payload.completed_at, envelope.occurred_at)
        started_raw = _extra(payload, "started_at") or _extra(payload, "arrival_at")
        started_at = _moment(started_raw, completed_at)
        wait_raw = _extra(payload, "wait_minutes")
        if wait_raw is not None:
            wait_minutes = max(0.0, float(wait_raw))
        else:
            wait_minutes = max(0.0, (completed_at - started_at).total_seconds() / 60.0)
        session.add(
            EncounterFact(
                event_id=envelope.event_id,
                encounter_id=payload.encounter_id,
                patient_id=payload.patient_id,
                department_id=_extra(payload, "department_id"),
                started_at=started_at,
                completed_at=completed_at,
                wait_minutes=wait_minutes,
            )
        )
        await session.commit()


async def _handle_invoice_issued(envelope, payload: InvoiceIssued) -> None:
    async with SessionLocal() as session:
        exists = await session.scalar(
            select(InvoiceFact.id).where(InvoiceFact.event_id == envelope.event_id)
        )
        if exists is not None:
            return
        session.add(
            InvoiceFact(
                event_id=envelope.event_id,
                invoice_id=payload.invoice_id,
                patient_id=payload.patient_id,
                department_id=_extra(payload, "department_id"),
                total_amount=payload.total_amount,
                patient_share=payload.patient_share,
                insurer_share=payload.insurer_share,
                currency=payload.currency or "USD",
                issued_at=_moment(payload.issued_at, envelope.occurred_at),
            )
        )
        await session.commit()


async def _handle_payment_completed(envelope, payload: PaymentCompleted) -> None:
    async with SessionLocal() as session:
        facts = (
            await session.execute(select(InvoiceFact).where(InvoiceFact.invoice_id == payload.invoice_id))
        ).scalars().all()
        if not facts:
            logger.info("Payment for unknown invoice %s ignored", payload.invoice_id)
            return
        for fact in facts:
            fact.paid_at = _moment(payload.paid_at, envelope.occurred_at)
            fact.paid_amount = payload.amount
        await session.commit()


async def handle_event(raw: dict[str, Any]) -> None:
    envelope, payload = parse_event(raw)
    if isinstance(payload, AppointmentBooked):
        await _handle_appointment_booked(envelope, payload)
    elif isinstance(payload, AppointmentCancelled):
        await _handle_appointment_cancelled(envelope, payload)
    elif isinstance(payload, EncounterCompleted):
        await _handle_encounter_completed(envelope, payload)
    elif isinstance(payload, InvoiceIssued):
        await _handle_invoice_issued(envelope, payload)
    elif isinstance(payload, PaymentCompleted):
        await _handle_payment_completed(envelope, payload)
    else:
        logger.debug("Ignoring analytics event %s", envelope.event_type)


def build_consumer() -> EventConsumer:
    return EventConsumer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        topics=[settings.kafka_topic],
        handler=handle_event,
        client_id=settings.service_name,
    )
