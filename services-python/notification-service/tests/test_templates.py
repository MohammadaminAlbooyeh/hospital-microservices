from __future__ import annotations

from app.domain.notifications import plan_notification, reminder_plan
from app.domain.templates import render_template


def test_render_welcome_template():
    body = render_template(
        "welcome.txt",
        {"full_name": "Jane Doe", "mrn": "MRN-123"},
    )
    assert "Jane Doe" in body
    assert "MRN-123" in body


def test_render_invoice_template():
    body = render_template(
        "invoice.txt",
        {
            "full_name": "Jane Doe",
            "invoice_id": "INV-1",
            "total_amount": 100.0,
            "patient_share": 20.0,
            "insurer_share": 80.0,
        },
    )
    assert "INV-1" in body
    assert "100.0" in body


def test_plan_notification_appointment_booked_prefers_sms():
    plan = plan_notification(
        "AppointmentBooked",
        {
            "appointment_id": "APT-1",
            "first_name": "Jane",
            "last_name": "Doe",
            "email": "jane@example.com",
            "phone": "+15551234567",
        },
    )
    assert plan is not None
    assert plan.channel == "SMS"
    assert plan.recipient == "+15551234567"
    assert plan.template == "appointment_confirmation.txt"


def test_plan_notification_returns_none_without_contact():
    plan = plan_notification("LabResultReady", {"lab_order_id": "LAB-1"})
    assert plan is None


def test_reminder_plan_prefers_phone():
    plan = reminder_plan(
        {
            "appointment_id": "APT-1",
            "first_name": "Jane",
            "last_name": "Doe",
            "phone": "+15551234567",
            "slot_start": "2026-11-06T10:00:00Z",
        }
    )
    assert plan is not None
    assert plan.channel == "SMS"
    assert plan.template == "appointment_reminder.txt"
    body = render_template(plan.template, plan.context)
    assert "APT-1" in body
