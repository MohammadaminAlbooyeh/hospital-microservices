from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum


class AuditAction(str, Enum):
    """The kind of operation performed against a resource."""

    CREATE = "CREATE"
    READ = "READ"
    UPDATE = "UPDATE"
    DELETE = "DELETE"


@dataclass(frozen=True)
class AuditLogEntry:
    """An immutable record of who did what to whose data, and when.

    Append-only by design: there are no update/delete use cases for this
    domain object, only creation (from a Kafka event or an explicit API
    call) and querying.
    """

    actor_id: str
    actor_role: str
    action: AuditAction
    resource_type: str
    resource_id: str
    source_service: str
    occurred_at: datetime
    correlation_id: str | None = None
    event_id: str | None = None
    details: str | None = None
    id: uuid.UUID = field(default_factory=uuid.uuid4)

    def __post_init__(self) -> None:
        if not self.actor_id:
            raise ValueError("actor_id is required")
        if not self.resource_type:
            raise ValueError("resource_type is required")
        if not self.resource_id:
            raise ValueError("resource_id is required")
        if not self.source_service:
            raise ValueError("source_service is required")


def _as_utc(value: datetime | None) -> datetime:
    moment = value or datetime.now(timezone.utc)
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=timezone.utc)
    return moment


def build_entry_from_event(
    *,
    event_id: str | None,
    event_type: str,
    occurred_at: datetime | None,
    correlation_id: str | None,
    source_service: str,
    actor_id: str,
    actor_role: str,
    action: AuditAction,
    resource_type: str,
    resource_id: str,
    details: str | None = None,
) -> AuditLogEntry:
    """Build an AuditLogEntry representing a cross-service access to patient data.

    This is the single translation point between "a domain event happened
    somewhere in the platform" and "someone/something accessed/mutated a
    patient's data", which is what HIPAA/GDPR access logging cares about.
    """
    return AuditLogEntry(
        actor_id=actor_id,
        actor_role=actor_role,
        action=action,
        resource_type=resource_type,
        resource_id=resource_id,
        source_service=source_service,
        occurred_at=_as_utc(occurred_at),
        correlation_id=correlation_id,
        event_id=event_id,
        details=details or f"Derived from {event_type} event",
    )
