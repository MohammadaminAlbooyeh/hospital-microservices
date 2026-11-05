from __future__ import annotations

from dataclasses import dataclass
from typing import Any

EMAIL = "EMAIL"
SMS = "SMS"


@dataclass(frozen=True)
class NotificationPlan:
    event_type: str
    channel: str
    recipient: str
    subject: str
    template: str
    context: dict[str, Any]


def _clean(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _pick_contact(context: dict[str, Any], *, prefer_email: bool) -> tuple[str, str] | None:
    email = _clean(context.get("email"))
    phone = _clean(context.get("phone"))
    if prefer_email:
        if email:
            return EMAIL, email
        if phone:
            return SMS, phone
    else:
        if phone:
            return SMS, phone
        if email:
            return EMAIL, email
    return None


def _full_name(context: dict[str, Any]) -> str:
    first = _clean(context.get("first_name")) or ""
    last = _clean(context.get("last_name")) or ""
    return f"{first} {last}".strip() or "there"


def plan_notification(event_type: str, payload: dict[str, Any]) -> NotificationPlan | None:
    """Map a domain event to a channel, recipient, subject and template."""
    context = dict(payload)
    context.setdefault("event_type", event_type)
    context.setdefault("full_name", _full_name(context))

    if event_type == "PatientRegistered":
        contact = _pick_contact(context, prefer_email=True)
        if contact is None:
            return None
        channel, recipient = contact
        return NotificationPlan(
            event_type=event_type,
            channel=channel,
            recipient=recipient,
            subject="Welcome to Hospital",
            template="welcome.txt",
            context=context,
        )

    if event_type == "AppointmentBooked":
        contact = _pick_contact(context, prefer_email=False)
        if contact is None:
            return None
        channel, recipient = contact
        return NotificationPlan(
            event_type=event_type,
            channel=channel,
            recipient=recipient,
            subject="Appointment confirmed",
            template="appointment_confirmation.txt",
            context=context,
        )

    if event_type == "AppointmentCancelled":
        contact = _pick_contact(context, prefer_email=False)
        if contact is None:
            return None
        channel, recipient = contact
        return NotificationPlan(
            event_type=event_type,
            channel=channel,
            recipient=recipient,
            subject="Appointment cancelled",
            template="appointment_cancelled.txt",
            context=context,
        )

    if event_type == "LabResultReady":
        contact = _pick_contact(context, prefer_email=True)
        if contact is None:
            return None
        channel, recipient = contact
        return NotificationPlan(
            event_type=event_type,
            channel=channel,
            recipient=recipient,
            subject="Your lab results are ready",
            template="lab_result.txt",
            context=context,
        )

    if event_type == "InvoiceIssued":
        contact = _pick_contact(context, prefer_email=True)
        if contact is None:
            return None
        channel, recipient = contact
        return NotificationPlan(
            event_type=event_type,
            channel=channel,
            recipient=recipient,
            subject="New hospital invoice",
            template="invoice.txt",
            context=context,
        )

    if event_type == "PaymentCompleted":
        contact = _pick_contact(context, prefer_email=True)
        if contact is None:
            return None
        channel, recipient = contact
        return NotificationPlan(
            event_type=event_type,
            channel=channel,
            recipient=recipient,
            subject="Payment receipt",
            template="payment_receipt.txt",
            context=context,
        )

    if event_type == "PaymentFailed":
        contact = _pick_contact(context, prefer_email=True)
        if contact is None:
            return None
        channel, recipient = contact
        return NotificationPlan(
            event_type=event_type,
            channel=channel,
            recipient=recipient,
            subject="Payment failed",
            template="payment_failure.txt",
            context=context,
        )

    return None


def reminder_plan(payload: dict[str, Any]) -> NotificationPlan | None:
    """Build an appointment reminder plan (used by scheduled reminder jobs)."""
    context = dict(payload)
    context.setdefault("event_type", "AppointmentReminder")
    context.setdefault("full_name", _full_name(context))
    contact = _pick_contact(context, prefer_email=False)
    if contact is None:
        return None
    channel, recipient = contact
    return NotificationPlan(
        event_type="AppointmentReminder",
        channel=channel,
        recipient=recipient,
        subject="Appointment reminder",
        template="appointment_reminder.txt",
        context=context,
    )
