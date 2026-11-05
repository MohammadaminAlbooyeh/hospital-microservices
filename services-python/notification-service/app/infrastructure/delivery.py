from __future__ import annotations

import asyncio
import logging
from datetime import datetime, timezone

from sqlalchemy.ext.asyncio import AsyncSession

from app.channels import deliver
from app.infrastructure.db import Notification

logger = logging.getLogger(__name__)


async def deliver_notification(session: AsyncSession, notification: Notification) -> bool:
    """Deliver a persisted notification and store the outcome."""
    result = await asyncio.to_thread(
        deliver,
        notification.channel,
        notification.recipient,
        notification.subject,
        notification.body,
    )
    if result.success:
        notification.status = "SENT"
        notification.sent_at = datetime.now(timezone.utc)
        notification.error = None
    else:
        notification.status = "FAILED"
        notification.sent_at = None
        notification.error = result.detail or "delivery failed"

    await session.commit()
    await session.refresh(notification)
    logger.info(
        "Notification %s via %s marked %s",
        notification.id,
        notification.channel,
        notification.status,
    )
    return result.success
