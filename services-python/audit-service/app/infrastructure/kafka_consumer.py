from __future__ import annotations

import logging
from typing import Any, Callable

from pydantic import ValidationError
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.domain.audit_entry import AuditAction, build_entry_from_event
from app.infrastructure.db import SessionLocal
from app.infrastructure.repository import AuditLogRepository
from hospital_shared.events.models import parse_event
from hospital_shared.kafka import EventConsumer

logger = logging.getLogger(__name__)

SessionFactory = Callable[[], AsyncSession]

# Every event type the platform publishes that touches patient data maps to
# one audit entry: (resource_type, action, patient_id attribute, service that
# raised the event, role of the actor that performed it).
_EVENT_AUDIT_MAP: dict[str, tuple[str, AuditAction, str]] = {
    "PatientRegistered": ("patient", AuditAction.CREATE, "patient-service"),
    "AppointmentBooked": ("patient", AuditAction.UPDATE, "scheduling-service"),
    "AppointmentCancelled": ("patient", AuditAction.UPDATE, "scheduling-service"),
    "EncounterCompleted": ("patient", AuditAction.UPDATE, "clinical-service"),
    "LabResultReady": ("patient", AuditAction.CREATE, "lab-service"),
    "InvoiceIssued": ("patient", AuditAction.CREATE, "billing-service"),
    "PaymentCompleted": ("patient", AuditAction.UPDATE, "billing-service"),
    "PaymentFailed": ("patient", AuditAction.UPDATE, "billing-service"),
}


def _patient_id(payload: Any) -> str | None:
    return getattr(payload, "patient_id", None)


async def handle_event(
    event: dict[str, Any],
    *,
    session_factory: SessionFactory = SessionLocal,
) -> None:
    try:
        envelope, payload = parse_event(event)
    except ValidationError as exc:
        logger.warning("Discarding invalid event: %s", exc)
        return

    mapping = _EVENT_AUDIT_MAP.get(envelope.event_type)
    if mapping is None or payload is None:
        logger.debug("Audit service ignoring event %s", envelope.event_type)
        return

    resource_type, action, source_service = mapping
    patient_id = _patient_id(payload)
    if not patient_id:
        logger.warning(
            "Event %s (%s) has no patient_id; skipping audit entry", envelope.event_type, envelope.event_id
        )
        return

    entry = build_entry_from_event(
        event_id=envelope.event_id,
        event_type=envelope.event_type,
        occurred_at=envelope.occurred_at,
        correlation_id=envelope.correlation_id,
        source_service=source_service,
        actor_id=source_service,
        actor_role="SERVICE",
        action=action,
        resource_type=resource_type,
        resource_id=patient_id,
        details=f"{source_service} published {envelope.event_type} for patient {patient_id}",
    )

    async with session_factory() as session:
        repository = AuditLogRepository(session)
        record = await repository.append(entry)
        if record is None:
            logger.info("Audit entry for event %s already recorded; skipping", envelope.event_id)
        else:
            logger.info(
                "Recorded audit entry %s: %s %s %s by %s",
                record.id,
                action.value,
                resource_type,
                patient_id,
                source_service,
            )


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
