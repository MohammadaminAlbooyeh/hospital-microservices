from __future__ import annotations

import json
import logging
from typing import Any

from aiokafka import AIOKafkaProducer

from hospital_shared.events.models import EventEnvelope

logger = logging.getLogger(__name__)


class EventProducer:
    """Thin async wrapper around AIOKafkaProducer with JSON serialisation."""

    def __init__(
        self,
        bootstrap_servers: str,
        topic: str = "hospital.events",
        client_id: str = "hospital-python",
    ) -> None:
        self._topic = topic
        self._client_id = client_id
        self._producer = AIOKafkaProducer(
            bootstrap_servers=bootstrap_servers,
            client_id=client_id,
            enable_idempotence=True,
            acks="all",
        )

    async def start(self) -> None:
        await self._producer.start()

    async def stop(self) -> None:
        await self._producer.stop()

    async def publish(self, event: EventEnvelope | dict[str, Any], key: str | None = None) -> None:
        if isinstance(event, EventEnvelope):
            payload = event.model_dump(mode="json")
            aggregate_id = event.aggregate_id
        else:
            payload = event
            aggregate_id = str(event.get("aggregate_id", ""))
        body = json.dumps(payload, default=str).encode("utf-8")
        await self._producer.send_and_wait(
            self._topic,
            key=(key or aggregate_id).encode("utf-8") if (key or aggregate_id) else None,
            value=body,
        )
        logger.info("Published event %s to %s", payload.get("event_type"), self._topic)
