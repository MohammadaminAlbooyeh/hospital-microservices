from __future__ import annotations

from datetime import datetime, timezone
from unittest.mock import patch

import pytest
from sqlalchemy import select
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.channels import DeliveryResult
from app.infrastructure.db import Base, Notification
from app.infrastructure.kafka_consumer import handle_event

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


def _appointment_booked_event(event_id: str = "evt-1") -> dict:
    return {
        "eventId": event_id,
        "eventType": "AppointmentBooked",
        "occurredAt": NOW.isoformat(),
        "aggregateId": "APT-1",
        "payload": {
            "appointmentId": "APT-1",
            "patientId": "PAT-1",
            "doctorId": "DOC-1",
            "slotStart": NOW.isoformat(),
            "email": "jane@example.com",
            "phone": "+15551234567",
            "firstName": "Jane",
            "lastName": "Doe",
        },
    }


async def test_handle_event_dispatches_sms_notification(session_factory):
    with patch(
        "app.channels.sms.send_sms",
        return_value=DeliveryResult(True),
    ) as mock_send_sms:
        await handle_event(_appointment_booked_event(), session_factory=session_factory)

    async with session_factory() as session:
        notifications = (await session.scalars(select(Notification))).all()

    assert len(notifications) == 1
    notification = notifications[0]
    assert notification.channel == "SMS"
    assert notification.recipient == "+15551234567"
    assert notification.event_type == "AppointmentBooked"
    assert notification.status == "SENT"
    mock_send_sms.assert_called_once()


async def test_handle_event_is_idempotent_on_event_id(session_factory):
    event = _appointment_booked_event(event_id="evt-dup")
    with patch("app.channels.sms.send_sms", return_value=DeliveryResult(True)):
        await handle_event(event, session_factory=session_factory)
        await handle_event(event, session_factory=session_factory)

    async with session_factory() as session:
        notifications = (await session.scalars(select(Notification))).all()

    assert len(notifications) == 1


async def test_handle_event_ignores_unknown_event_type(session_factory):
    event = {
        "eventId": "evt-2",
        "eventType": "SomethingElseHappened",
        "occurredAt": NOW.isoformat(),
        "aggregateId": "X-1",
        "payload": {},
    }
    await handle_event(event, session_factory=session_factory)

    async with session_factory() as session:
        notifications = (await session.scalars(select(Notification))).all()

    assert notifications == []


async def test_handle_event_skips_when_no_contact_info(session_factory):
    event = {
        "eventId": "evt-3",
        "eventType": "LabResultReady",
        "occurredAt": NOW.isoformat(),
        "aggregateId": "LAB-1",
        "payload": {
            "labOrderId": "LAB-1",
            "patientId": "PAT-1",
            "testCode": "CBC",
        },
    }
    await handle_event(event, session_factory=session_factory)

    async with session_factory() as session:
        notifications = (await session.scalars(select(Notification))).all()

    assert notifications == []
