from __future__ import annotations

import logging
import smtplib
from email.message import EmailMessage

from app.channels import DeliveryResult
from app.core.config import settings

logger = logging.getLogger(__name__)


def send_email(recipient: str, subject: str, body: str) -> DeliveryResult:
    if not settings.smtp_host:
        logger.warning("SMTP is not configured; email to %s was not sent", recipient)
        return DeliveryResult(False, "SMTP is not configured")

    message = EmailMessage()
    message["From"] = settings.smtp_from
    message["To"] = recipient
    message["Subject"] = subject
    message.set_content(body)

    try:
        with smtplib.SMTP(
            host=settings.smtp_host,
            port=settings.smtp_port,
            timeout=settings.smtp_timeout,
        ) as smtp:
            smtp.ehlo()
            if settings.smtp_use_tls:
                smtp.starttls()
                smtp.ehlo()
            if settings.smtp_username:
                smtp.login(settings.smtp_username, settings.smtp_password or "")
            smtp.send_message(message)
    except Exception as exc:
        logger.warning("SMTP delivery to %s failed: %s", recipient, exc)
        return DeliveryResult(False, str(exc))

    logger.info("Email sent to %s", recipient)
    return DeliveryResult(True)
