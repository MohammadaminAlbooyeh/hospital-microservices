from __future__ import annotations

from datetime import datetime, timezone

import pytest

from app.domain.audit_entry import AuditAction, AuditLogEntry, build_entry_from_event


def test_audit_log_entry_requires_core_fields():
    with pytest.raises(ValueError):
        AuditLogEntry(
            actor_id="",
            actor_role="SERVICE",
            action=AuditAction.READ,
            resource_type="patient",
            resource_id="PAT-1",
            source_service="lab-service",
            occurred_at=datetime.now(timezone.utc),
        )


def test_audit_log_entry_is_immutable():
    entry = AuditLogEntry(
        actor_id="lab-service",
        actor_role="SERVICE",
        action=AuditAction.CREATE,
        resource_type="patient",
        resource_id="PAT-1",
        source_service="lab-service",
        occurred_at=datetime.now(timezone.utc),
    )
    with pytest.raises(AttributeError):
        entry.resource_id = "PAT-2"  # type: ignore[misc]


def test_build_entry_from_event_defaults_details_and_normalises_timezone():
    naive_time = datetime(2026, 1, 1, 12, 0, 0)
    entry = build_entry_from_event(
        event_id="EVT-1",
        event_type="LabResultReady",
        occurred_at=naive_time,
        correlation_id="corr-1",
        source_service="lab-service",
        actor_id="lab-service",
        actor_role="SERVICE",
        action=AuditAction.CREATE,
        resource_type="patient",
        resource_id="PAT-9",
    )
    assert entry.occurred_at.tzinfo is not None
    assert entry.event_id == "EVT-1"
    assert entry.correlation_id == "corr-1"
    assert "LabResultReady" in entry.details


def test_build_entry_from_event_uses_now_when_occurred_at_missing():
    entry = build_entry_from_event(
        event_id=None,
        event_type="ManualEntry",
        occurred_at=None,
        correlation_id=None,
        source_service="compliance-tool",
        actor_id="officer-1",
        actor_role="COMPLIANCE_OFFICER",
        action=AuditAction.READ,
        resource_type="patient",
        resource_id="PAT-2",
    )
    assert entry.occurred_at.tzinfo is not None
    assert entry.event_id is None
