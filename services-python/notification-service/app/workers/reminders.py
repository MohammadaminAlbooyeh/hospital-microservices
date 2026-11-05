from __future__ import annotations

import logging
import uuid
from typing import Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.domain.notifications import reminder_plan
from app.domain.templates import render_template
from app.infrastructure.db import Notification, SessionLocal
from app.infrastructure.delivery import deliver_notification

logger = logging.getLogger(__name__)


async def send_appointment_reminder(
    appointment: dict[str, Any],
    *,
    session_factory=SessionLocal,
) -> bool:
    """Send a single appointment reminder (24h-before job).

    `appointment` carries at least appointment_id, full_name/first_name/last_name,
    email/phone, slot_start, doctor_id. Idempotent on appointment_id: a reminder is
    only ever recorded once per appointment.
    """
    plan = reminder_plan(appointment)
    if plan is None:
        logger.info(
            "No contact info available to remind for appointment %s; skipping",
            appointment.get("appointment_id"),
        )
        return False

    event_id = f"AppointmentReminder:{appointment.get('appointment_id')}"

    async with session_factory() as session:
        existing = await session.scalar(
            select(Notification.id).where(Notification.event_id == event_id)
        )
        if existing is not None:
            logger.info("Reminder for appointment %s already sent; skipping", event_id)
            return False

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

        return await deliver_notification(session, notification)


async def send_due_reminders(appointments: list[dict[str, Any]]) -> int:
    """Send reminders for a batch of appointments due in ~24h.

    `appointments` would normally come from a query against appointment-service
    (or a local read-model) for slots starting in the next reminder window. This
    service has no own scheduler process; invoke this from an external
    cron/k8s CronJob or APScheduler-style trigger hitting an admin endpoint.
    """
    sent = 0
    for appointment in appointments:
        if await send_appointment_reminder(appointment):
            sent += 1
    return sent
