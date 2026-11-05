from __future__ import annotations

from datetime import date, datetime, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.v1.schemas import AuditEventIn, AuditLogEntryResponse, AuditLogPage
from app.domain.audit_entry import AuditAction, build_entry_from_event
from app.infrastructure.db import get_session
from app.infrastructure.repository import AuditLogRepository
from hospital_shared.errors import AppError

router = APIRouter(prefix="/api/v1/audit", tags=["audit"])


def _bounds(from_date: date | None, to_date: date | None) -> tuple[datetime | None, datetime | None]:
    if from_date and to_date and from_date > to_date:
        raise AppError("from date must not be after to date")
    start = datetime(from_date.year, from_date.month, from_date.day, tzinfo=timezone.utc) if from_date else None
    end = (
        datetime(to_date.year, to_date.month, to_date.day, 23, 59, 59, tzinfo=timezone.utc)
        if to_date
        else None
    )
    return start, end


@router.post("/events", response_model=AuditLogEntryResponse)
async def record_event(
    payload: AuditEventIn, session: AsyncSession = Depends(get_session)
) -> AuditLogEntryResponse:
    entry = build_entry_from_event(
        event_id=None,
        event_type="ManualEntry",
        occurred_at=datetime.now(timezone.utc),
        correlation_id=payload.correlation_id,
        source_service=payload.source_service,
        actor_id=payload.actor_id,
        actor_role=payload.actor_role,
        action=payload.action,
        resource_type=payload.resource_type,
        resource_id=payload.resource_id,
        details=payload.details,
    )
    repository = AuditLogRepository(session)
    record = await repository.append(entry)
    if record is None:
        raise AppError("Duplicate audit event", status_code=409, code="CONFLICT")
    return AuditLogEntryResponse.model_validate(record)


@router.get("/events", response_model=AuditLogPage)
async def list_events(
    patient_id: str | None = Query(default=None, description="Filter by patient id (resource_id)"),
    user_id: str | None = Query(default=None, description="Filter by actor id"),
    action: AuditAction | None = Query(default=None),
    source_service: str | None = Query(default=None),
    from_date: date | None = Query(default=None, alias="from"),
    to_date: date | None = Query(default=None, alias="to"),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=50, ge=1, le=500),
    session: AsyncSession = Depends(get_session),
) -> AuditLogPage:
    """Read-only, paginated audit trail query for compliance officers."""
    start, end = _bounds(from_date, to_date)
    repository = AuditLogRepository(session)
    rows, total = await repository.query(
        patient_id=patient_id,
        user_id=user_id,
        action=action.value if action else None,
        source_service=source_service,
        from_date=start,
        to_date=end,
        page=page,
        page_size=page_size,
    )
    return AuditLogPage(
        items=[AuditLogEntryResponse.model_validate(row) for row in rows],
        page=page,
        page_size=page_size,
        total=total,
    )
