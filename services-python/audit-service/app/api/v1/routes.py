from __future__ import annotations

from fastapi import APIRouter
from pydantic import BaseModel, Field

router = APIRouter(prefix="/api/v1/audit", tags=["audit"])
_AUDIT_BUFFER: list[dict[str, str]] = []


class AuditEventIn(BaseModel):
    actor: str = Field(min_length=1)
    action: str = Field(min_length=1)
    resource_type: str = Field(min_length=1)
    resource_id: str = Field(min_length=1)
    outcome: str = "SUCCESS"


@router.post("/events")
async def record_event(payload: AuditEventIn) -> dict[str, str]:
    _AUDIT_BUFFER.append(payload.model_dump())
    return {"status": "recorded"}


@router.get("/events")
async def list_events() -> list[dict[str, str]]:
    # Dev skeleton: immutable persistence/Kafka ingestion is handled by migrations/consumers.
    return list(_AUDIT_BUFFER)
