from __future__ import annotations

import logging

import requests

from app.channels import DeliveryResult
from app.core.config import settings

logger = logging.getLogger(__name__)


def _twilio_configured() -> bool:
    return bool(
        settings.twilio_account_sid
        and settings.twilio_auth_token
        and settings.twilio_from_number
    )


def send_sms(recipient: str, body: str) -> DeliveryResult:
    if not _twilio_configured():
        logger.warning("Twilio is not configured; SMS to %s was not sent", recipient)
        logger.debug("SMS body for %s: %s", recipient, body)
        return DeliveryResult(False, "Twilio is not configured")

    url = (
        "https://api.twilio.com/2010-04-01/Accounts/"
        f"{settings.twilio_account_sid}/Messages.json"
    )
    try:
        response = requests.post(
            url,
            auth=(settings.twilio_account_sid, settings.twilio_auth_token),
            data={
                "From": settings.twilio_from_number,
                "To": recipient,
                "Body": body,
            },
            timeout=settings.twilio_timeout,
        )
    except requests.RequestException as exc:
        logger.warning("SMS delivery to %s failed: %s", recipient, exc)
        return DeliveryResult(False, str(exc))

    if not response.ok:
        detail = f"Twilio returned {response.status_code}: {response.text[:200]}"
        logger.warning("SMS delivery to %s failed: %s", recipient, detail)
        return DeliveryResult(False, detail)

    logger.info("SMS sent to %s", recipient)
    return DeliveryResult(True)
