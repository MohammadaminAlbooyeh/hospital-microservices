from __future__ import annotations

from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.domain.audit_entry import AuditAction


class AuditLogEntryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    event_id: str | None
    actor_id: str
    actor_role: str
    action: AuditAction
    resource_type: str
    resource_id: str
    source_service: str
    correlation_id: str | None
    details: str | None
    occurred_at: datetime
    recorded_at: datetime


class AuditLogPage(BaseModel):
    items: list[AuditLogEntryResponse]
    page: int
    page_size: int
    total: int


class AuditEventIn(BaseModel):
    """Manual audit entry submission, e.g. from a service that cannot publish
    to Kafka directly (admin tools, one-off scripts)."""

    actor_id: str = Field(min_length=1, max_length=64)
    actor_role: str = Field(min_length=1, max_length=64)
    action: AuditAction
    resource_type: str = Field(min_length=1, max_length=64)
    resource_id: str = Field(min_length=1, max_length=64)
    source_service: str = Field(min_length=1, max_length=64)
    correlation_id: str | None = None
    details: str | None = None
