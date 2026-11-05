from __future__ import annotations

import asyncio
import logging

from app.core.config import settings

logger = logging.getLogger(__name__)


class NotificationConsumer:
    async def start(self) -> None:
        if not settings.kafka_consumer_enabled:
            return
        logger.info("Notification Kafka consumer enabled (bootstrap=%s topic=%s)", settings.kafka_bootstrap_servers, settings.kafka_topic)

    async def stop(self) -> None:
        await asyncio.sleep(0)


consumer = NotificationConsumer()
