from __future__ import annotations

from datetime import datetime, timezone
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.channels import DeliveryResult
from app.infrastructure.db import Base, Notification
from app.workers.reminders import send_appointment_reminder, send_due_reminders

NOW = datetime.now(timezone.utc)


@pytest.fixture
async def session_factory():
    engine = create_async_engine(
        "sqlite+aiosqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    maker = async_sessionmaker(engine, expire_on_commit=False)
    try:
        yield maker
    finally:
        await engine.dispose()


def _appointment(appointment_id: str = "APT-1") -> dict:
    return {
        "appointment_id": appointment_id,
        "first_name": "Jane",
        "last_name": "Doe",
        "email": "jane@example.com",
        "phone": "+15551234567",
        "slot_start": NOW.isoformat(),
        "doctor_id": "DOC-1",
    }


async def test_send_appointment_reminder_delivers_once(session_factory):
    with patch("app.channels.sms.send_sms", return_value=DeliveryResult(True)) as mock_send_sms:
        sent = await send_appointment_reminder(_appointment(), session_factory=session_factory)

    assert sent is True
    mock_send_sms.assert_called_once()
    async with session_factory() as session:
        notifications = (await session.scalars(select(Notification))).all()
    assert len(notifications) == 1
    assert notifications[0].event_type == "AppointmentReminder"


async def test_send_appointment_reminder_is_idempotent(session_factory):
    appointment = _appointment(appointment_id="APT-dup")
    with patch("app.channels.sms.send_sms", return_value=DeliveryResult(True)):
        first = await send_appointment_reminder(appointment, session_factory=session_factory)
        second = await send_appointment_reminder(appointment, session_factory=session_factory)

    assert first is True
    assert second is False
    async with session_factory() as session:
        notifications = (await session.scalars(select(Notification))).all()
    assert len(notifications) == 1


async def test_send_appointment_reminder_skips_without_contact_info(session_factory):
    appointment = {"appointment_id": "APT-no-contact", "slot_start": NOW.isoformat()}
    sent = await send_appointment_reminder(appointment, session_factory=session_factory)

    assert sent is False
    async with session_factory() as session:
        notifications = (await session.scalars(select(Notification))).all()
    assert notifications == []


async def test_send_due_reminders_counts_successes(session_factory):
    appointments = [_appointment("APT-1"), _appointment("APT-2"), {"appointment_id": "APT-no-contact"}]
    with patch("app.channels.sms.send_sms", return_value=DeliveryResult(True)):
        sent = await send_due_reminders(appointments, session_factory=session_factory)

    assert sent == 2


def test_run_reminders_endpoint_delegates_to_worker():
    from app import main

    async def fake_send_due_reminders(appointments):
        assert appointments == [
            {
                "appointment_id": "APT-1",
                "first_name": "Jane",
                "last_name": "Doe",
                "email": "jane@example.com",
                "phone": None,
                "slot_start": None,
                "doctor_id": None,
            }
        ]
        return 1

    with patch("app.api.v1.routes.send_due_reminders", fake_send_due_reminders):
        client = TestClient(main.app)
        resp = client.post(
            "/api/v1/notifications/internal/reminders/run",
            json={
                "appointments": [
                    {
                        "appointment_id": "APT-1",
                        "first_name": "Jane",
                        "last_name": "Doe",
                        "email": "jane@example.com",
                    }
                ]
            },
        )

    assert resp.status_code == 200
    assert resp.json() == {"considered": 1, "sent": 1}
