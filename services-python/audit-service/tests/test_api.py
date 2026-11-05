from __future__ import annotations

import pytest
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.infrastructure.db import Base, get_session
from app.main import app


@pytest.fixture
async def client():
    engine = create_async_engine(
        "sqlite+aiosqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    maker = async_sessionmaker(engine, expire_on_commit=False, class_=AsyncSession)

    async def _override_get_session():
        async with maker() as session:
            yield session

    app.dependency_overrides[get_session] = _override_get_session
    try:
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as async_client:
            yield async_client
    finally:
        app.dependency_overrides.pop(get_session, None)
        await engine.dispose()


async def test_record_and_list_events(client):
    payload = {
        "actor_id": "officer-1",
        "actor_role": "COMPLIANCE_OFFICER",
        "action": "READ",
        "resource_type": "patient",
        "resource_id": "PAT-1",
        "source_service": "staff-console",
    }
    create_resp = await client.post("/api/v1/audit/events", json=payload)
    assert create_resp.status_code == 200
    body = create_resp.json()
    assert body["resource_id"] == "PAT-1"
    assert body["action"] == "READ"

    list_resp = await client.get("/api/v1/audit/events", params={"patient_id": "PAT-1"})
    assert list_resp.status_code == 200
    page = list_resp.json()
    assert page["total"] == 1
    assert page["items"][0]["resource_id"] == "PAT-1"


async def test_list_events_filters_by_action_and_paginates(client):
    base = {
        "actor_role": "COMPLIANCE_OFFICER",
        "resource_type": "patient",
        "source_service": "staff-console",
    }
    for i in range(3):
        await client.post(
            "/api/v1/audit/events",
            json={**base, "actor_id": f"officer-{i}", "action": "READ", "resource_id": "PAT-2"},
        )
    await client.post(
        "/api/v1/audit/events",
        json={**base, "actor_id": "officer-9", "action": "UPDATE", "resource_id": "PAT-2"},
    )

    read_resp = await client.get(
        "/api/v1/audit/events", params={"patient_id": "PAT-2", "action": "READ", "page_size": 2}
    )
    page = read_resp.json()
    assert page["total"] == 3
    assert len(page["items"]) == 2

    update_resp = await client.get(
        "/api/v1/audit/events", params={"patient_id": "PAT-2", "action": "UPDATE"}
    )
    assert update_resp.json()["total"] == 1


async def test_health_endpoint(client):
    resp = await client.get("/health")
    assert resp.status_code == 200
