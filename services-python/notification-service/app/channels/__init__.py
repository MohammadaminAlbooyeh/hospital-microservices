from __future__ import annotations

import logging
from dataclasses import dataclass

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class DeliveryResult:
    success: bool
    detail: str | None = None


def deliver(channel: str, recipient: str, subject: str, body: str) -> DeliveryResult:
    from app.channels.email import send_email
    from app.channels.sms import send_sms

    normalised = (channel or "").upper()
    if normalised == "EMAIL":
        return send_email(recipient, subject, body)
    if normalised == "SMS":
        return send_sms(recipient, body)

    logger.warning("Push delivery requested for %s but no push provider is configured", recipient)
    return DeliveryResult(False, "PUSH delivery is not configured")
