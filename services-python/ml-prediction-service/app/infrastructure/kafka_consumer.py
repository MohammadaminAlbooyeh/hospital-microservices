from __future__ import annotations

import logging
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from hospital_shared.kafka import EventConsumer

from app.core.config import settings
from app.domain.no_show_predictor import NoShowPredictor
from app.infrastructure.risk_cache import CachedRisk, risk_cache

logger = logging.getLogger(__name__)

_model_dir = Path(settings.model_dir)
_no_show_predictor = NoShowPredictor(_model_dir / settings.no_show_model_file)


def _slot_start(payload: dict[str, Any]) -> datetime | None:
    slot_start = payload.get("slotStart") or payload.get("slot_start")
    if slot_start is None:
        return None
    try:
        # Avro timestamp-millis arrives as epoch millis once decoded to JSON.
        return datetime.fromtimestamp(float(slot_start) / 1000.0, tz=timezone.utc)
    except (TypeError, ValueError):
        return None


def _booked_at(payload: dict[str, Any]) -> datetime:
    booked_at = payload.get("bookedAt") or payload.get("booked_at")
    if booked_at is not None:
        try:
            return datetime.fromtimestamp(float(booked_at) / 1000.0, tz=timezone.utc)
        except (TypeError, ValueError):
            pass
    return datetime.now(timezone.utc)


def _features_from_appointment_booked(payload: dict[str, Any]) -> dict[str, Any] | None:
    slot_start = _slot_start(payload)
    if slot_start is None:
        return None
    booked_at = _booked_at(payload)
    lead_days = max(0, (slot_start - booked_at).days)
    return {
        # No per-patient history is available on this event alone, so default
        # to conservative, population-level values for the fields this
        # service does not own; a richer implementation would look these up
        # from patient-service/appointment history instead.
        "age": 40,
        "lead_days": lead_days,
        "prior_no_shows": 0,
        "distance_km": 5.0,
        "appointment_hour": slot_start.hour,
        "day_of_week": slot_start.weekday(),
        "has_insurance": True,
        "is_new_patient": False,
    }


async def handle_event(event: dict[str, Any]) -> None:
    event_type = event.get("event_type") or event.get("eventType")
    payload = event.get("payload", event)

    if event_type != "AppointmentBooked":
        logger.debug("Ignoring %s", event_type)
        return

    appointment_id = payload.get("appointmentId") or payload.get("appointment_id")
    patient_id = payload.get("patientId") or payload.get("patient_id")
    if not appointment_id:
        logger.warning("AppointmentBooked event missing appointmentId; skipping")
        return

    features = _features_from_appointment_booked(payload)
    if features is None:
        logger.warning("AppointmentBooked event %s missing slotStart; skipping", appointment_id)
        return

    risk, model_used = _no_show_predictor.predict(features)
    risk_cache.set(
        CachedRisk(
            appointment_id=str(appointment_id),
            patient_id=str(patient_id) if patient_id else "",
            risk=risk,
            model_used=model_used,
        )
    )
    logger.info(
        "Computed no-show risk %.4f (%s) for appointment %s",
        risk,
        model_used,
        appointment_id,
    )


def build_consumer() -> EventConsumer:
    return EventConsumer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        topics=[settings.kafka_topic],
        handler=handle_event,
        client_id=settings.service_name,
    )
