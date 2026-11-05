from __future__ import annotations

import logging
from typing import Any

from hospital_shared.kafka import EventConsumer

from app.core.config import settings

logger = logging.getLogger(__name__)


async def handle_event(event: dict[str, Any]) -> None:
    event_type = event.get("event_type")
    payload = event.get("payload", {})
    # Triage reacts to encounter completion by closing any open queue entry.
    if event_type == "EncounterCompleted":
        logger.info(
            "Encounter %s completed for patient %s; triage record can be closed",
            payload.get("encounterId") or payload.get("encounter_id"),
            payload.get("patientId") or payload.get("patient_id"),
        )
    else:
        logger.debug("Ignoring %s", event_type)


def build_consumer() -> EventConsumer:
    return EventConsumer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        topics=[settings.kafka_topic],
        handler=handle_event,
        client_id=settings.service_name,
    )
