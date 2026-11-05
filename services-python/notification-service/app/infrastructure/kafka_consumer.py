from __future__ import annotations

import logging
import uuid
from typing import Any, Callable

from pydantic import BaseModel, ValidationError
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.domain.notifications import plan_notification
from app.domain.templates import render_template
from app.infrastructure.db import Notification, SessionLocal
from app.infrastructure.delivery import deliver_notification
from hospital_shared.events.models import parse_event
from hospital_shared.kafka import EventConsumer

logger = logging.getLogger(__name__)

SessionFactory = Callable[[], AsyncSession]

# Events that notification-service reacts to by sending a notification directly.
# AppointmentCancelled's event_type matches its template 1:1 with plan_notification.
HANDLED_EVENTS = {
    "PatientRegistered",
    "AppointmentBooked",
    "AppointmentCancelled",
    "LabResultReady",
    "InvoiceIssued",
    "PaymentCompleted",
    "PaymentFailed",
}


async def _dispatch(
    event_type: str,
    event_id: str | None,
    payload: BaseModel,
    session_factory: Callable[[], AsyncSession],
) -> None:
    plan = plan_notification(event_type, payload.model_dump(mode="json"))
    if plan is None:
        logger.info(
            "No contact info available to notify for %s (event_id=%s); skipping",
            event_type,
            event_id,
        )
        return

    async with session_factory() as session:
        if event_id is not None:
            existing = await session.scalar(
                select(Notification.id).where(Notification.event_id == event_id)
            )
            if existing is not None:
                logger.info("Notification for event %s already recorded; skipping", event_id)
                return

        body = render_template(plan.template, plan.context)
        notification = Notification(
            id=uuid.uuid4(),
            event_id=event_id,
            channel=plan.channel,
            recipient=plan.recipient,
            subject=plan.subject,
            body=body,
            event_type=plan.event_type,
        )
        session.add(notification)
        await session.commit()
        await session.refresh(notification)

        await deliver_notification(session, notification)


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

    if envelope.event_type not in HANDLED_EVENTS or payload is None:
        logger.debug("Notification service ignoring event %s", envelope.event_type)
        return

    await _dispatch(envelope.event_type, envelope.event_id, payload, session_factory)


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


class NotificationConsumer:
    """Lifecycle wrapper used by the FastAPI app; starts/stops the Kafka consumer."""

    def __init__(self) -> None:
        self._consumer: EventConsumer | None = None

    async def start(self) -> None:
        if not settings.kafka_consumer_enabled:
            return
        self._consumer = build_consumer()
        await self._consumer.start()
        logger.info(
            "Notification Kafka consumer started (bootstrap=%s topic=%s)",
            settings.kafka_bootstrap_servers,
            settings.kafka_topic,
        )

    async def stop(self) -> None:
        if self._consumer is not None:
            await self._consumer.stop()
            self._consumer = None


consumer = NotificationConsumer()
