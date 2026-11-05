from __future__ import annotations

import asyncio
import json
import logging
from collections.abc import Awaitable, Callable
from typing import Any

from aiokafka import AIOKafkaConsumer

logger = logging.getLogger(__name__)

Handler = Callable[[dict[str, Any]], Awaitable[None]]


class EventConsumer:
    """At-least-once consumer. Handlers must be idempotent."""

    def __init__(
        self,
        bootstrap_servers: str,
        group_id: str,
        topics: list[str] | tuple[str, ...],
        handler: Handler,
        client_id: str | None = None,
    ) -> None:
        self._consumer = AIOKafkaConsumer(
            *topics,
            bootstrap_servers=bootstrap_servers,
            group_id=group_id,
            client_id=client_id or group_id,
            auto_offset_reset="earliest",
            enable_auto_commit=False,
        )
        self._handler = handler
        self._task: asyncio.Task[None] | None = None

    async def _run(self) -> None:
        await self._consumer.start()
        logger.info("Kafka consumer started: %s", self._consumer.subscription())
        try:
            async for message in self._consumer:
                try:
                    event = json.loads(message.value.decode("utf-8"))
                except json.JSONDecodeError:
                    logger.warning("Discarding non-JSON message at %s", message.offset)
                    await self._consumer.commit()
                    continue
                try:
                    await self._handler(event)
                    await self._consumer.commit()
                except Exception:
                    logger.exception(
                        "Handler failed for %s at offset %s; not committing",
                        event.get("event_type"),
                        message.offset,
                    )
                    await asyncio.sleep(1)
        finally:
            await self._consumer.stop()

    async def start(self) -> None:
        self._task = asyncio.create_task(self._run())

    async def stop(self) -> None:
        if self._task:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
