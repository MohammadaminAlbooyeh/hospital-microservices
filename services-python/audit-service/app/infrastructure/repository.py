from __future__ import annotations

from datetime import datetime

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.domain.audit_entry import AuditLogEntry
from app.infrastructure.db import AuditLogRecord


class AuditLogRepository:
    """Append-only access to the audit_log table.

    Deliberately exposes no update/delete methods: entries are created once
    and only ever queried afterwards.
    """

    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def append(self, entry: AuditLogEntry) -> AuditLogRecord | None:
        """Persist an entry. Returns None (and skips the insert) for a
        duplicate event_id, since Kafka delivery is at-least-once and
        handlers must be idempotent."""
        if entry.event_id is not None:
            existing = await self._session.scalar(
                select(AuditLogRecord.id).where(AuditLogRecord.event_id == entry.event_id)
            )
            if existing is not None:
                return None

        record = AuditLogRecord(
            id=entry.id,
            event_id=entry.event_id,
            actor_id=entry.actor_id,
            actor_role=entry.actor_role,
            action=entry.action.value,
            resource_type=entry.resource_type,
            resource_id=entry.resource_id,
            source_service=entry.source_service,
            correlation_id=entry.correlation_id,
            details=entry.details,
            occurred_at=entry.occurred_at,
        )
        self._session.add(record)
        try:
            await self._session.commit()
        except IntegrityError:
            await self._session.rollback()
            return None
        return record

    async def query(
        self,
        *,
        patient_id: str | None = None,
        user_id: str | None = None,
        action: str | None = None,
        source_service: str | None = None,
        from_date: datetime | None = None,
        to_date: datetime | None = None,
        page: int = 1,
        page_size: int = 50,
    ) -> tuple[list[AuditLogRecord], int]:
        """Filter and paginate audit entries for compliance review.

        `patient_id` filters on resource_id (the common case: resource_type
        "patient"), `user_id` filters on actor_id.
        """
        statement = select(AuditLogRecord)
        if patient_id is not None:
            statement = statement.where(AuditLogRecord.resource_id == patient_id)
        if user_id is not None:
            statement = statement.where(AuditLogRecord.actor_id == user_id)
        if action is not None:
            statement = statement.where(AuditLogRecord.action == action)
        if source_service is not None:
            statement = statement.where(AuditLogRecord.source_service == source_service)
        if from_date is not None:
            statement = statement.where(AuditLogRecord.occurred_at >= from_date)
        if to_date is not None:
            statement = statement.where(AuditLogRecord.occurred_at <= to_date)

        total = len((await self._session.execute(statement)).scalars().all())

        statement = (
            statement.order_by(AuditLogRecord.occurred_at.desc())
            .offset((page - 1) * page_size)
            .limit(page_size)
        )
        rows = (await self._session.execute(statement)).scalars().all()
        return list(rows), total
