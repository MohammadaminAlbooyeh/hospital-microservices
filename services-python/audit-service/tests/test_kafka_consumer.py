from __future__ import annotations

from datetime import datetime, timezone

import pytest
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.infrastructure.db import AuditLogRecord, Base
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


def lab_result_event(event_id: str, patient_id: str = "PAT-1") -> dict:
    return {
        "eventId": event_id,
        "eventType": "LabResultReady",
        "occurredAt": NOW.isoformat(),
        "aggregateId": "LAB-1",
        "correlationId": "corr-1",
        "payload": {
            "labOrderId": "LAB-1",
            "patientId": patient_id,
            "testCode": "CBC",
            "critical": False,
        },
    }


async def test_known_event_creates_one_audit_entry(session_factory):
    await handle_event(lab_result_event("EVT-1"), session_factory=session_factory)

    async with session_factory() as session:
        count = await session.scalar(select(func.count()).select_from(AuditLogRecord))
        record = await session.scalar(select(AuditLogRecord).where(AuditLogRecord.event_id == "EVT-1"))

    assert count == 1
    assert record is not None
    assert record.resource_id == "PAT-1"
    assert record.resource_type == "patient"
    assert record.action == "CREATE"
    assert record.source_service == "lab-service"
    assert record.correlation_id == "corr-1"


async def test_duplicate_event_is_not_double_recorded(session_factory):
    await handle_event(lab_result_event("EVT-2"), session_factory=session_factory)
    await handle_event(lab_result_event("EVT-2"), session_factory=session_factory)

    async with session_factory() as session:
        count = await session.scalar(select(func.count()).select_from(AuditLogRecord))

    assert count == 1


async def test_unmapped_event_type_is_ignored(session_factory):
    await handle_event(
        {
            "eventId": "EVT-3",
            "eventType": "SomeUnknownEvent",
            "occurredAt": NOW.isoformat(),
            "aggregateId": "X-1",
            "payload": {},
        },
        session_factory=session_factory,
    )

    async with session_factory() as session:
        count = await session.scalar(select(func.count()).select_from(AuditLogRecord))

    assert count == 0


async def test_event_without_patient_id_is_skipped(session_factory):
    await handle_event(
        {
            "eventId": "EVT-4",
            "eventType": "LabResultReady",
            "occurredAt": NOW.isoformat(),
            "aggregateId": "LAB-2",
            "payload": {"labOrderId": "LAB-2", "patientId": "", "testCode": "CBC"},
        },
        session_factory=session_factory,
    )

    async with session_factory() as session:
        count = await session.scalar(select(func.count()).select_from(AuditLogRecord))

    assert count == 0


async def test_invalid_event_is_ignored(session_factory):
    await handle_event({"eventType": "LabResultReady"}, session_factory=session_factory)

    async with session_factory() as session:
        count = await session.scalar(select(func.count()).select_from(AuditLogRecord))

    assert count == 0
