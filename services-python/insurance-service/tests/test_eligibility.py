from __future__ import annotations

from datetime import datetime, timedelta, timezone
from types import SimpleNamespace

from app.domain.adjudication import (
    ClaimStatus,
    auto_adjudicate,
    calculate_approved_amount,
    generate_claim_number,
    round_money,
)
from app.domain.eligibility import check_eligibility

NOW = datetime(2026, 6, 1, 12, 0, tzinfo=timezone.utc)


def policy(**overrides):
    values = {
        "member_id": "MEM-1",
        "active": True,
        "valid_from": NOW - timedelta(days=30),
        "valid_to": NOW + timedelta(days=30),
        "coverage_percent": 80,
        "policy_number": "POL-1",
    }
    values.update(overrides)
    return SimpleNamespace(**values)


def test_active_policy_inside_window_is_eligible():
    result = check_eligibility(policy(), "MEM-1", now=NOW)
    assert result.eligible is True
    assert result.coverage_percent == 80
    assert result.reason == "eligible"


def test_missing_policy_is_not_eligible():
    result = check_eligibility(None, "MEM-1", now=NOW)
    assert result.eligible is False
    assert result.coverage_percent == 0
    assert "not found" in result.reason


def test_member_mismatch_is_not_eligible():
    result = check_eligibility(policy(member_id="OTHER"), "MEM-1", now=NOW)
    assert result.eligible is False
    assert "member" in result.reason


def test_inactive_policy_is_not_eligible():
    result = check_eligibility(policy(active=False), "MEM-1", now=NOW)
    assert result.eligible is False
    assert "inactive" in result.reason


def test_policy_before_valid_from_is_not_eligible():
    result = check_eligibility(policy(valid_from=NOW + timedelta(days=1)), "MEM-1", now=NOW)
    assert result.eligible is False
    assert "not yet effective" in result.reason


def test_expired_policy_is_not_eligible():
    result = check_eligibility(policy(valid_to=NOW - timedelta(days=1)), "MEM-1", now=NOW)
    assert result.eligible is False
    assert "expired" in result.reason


def test_naive_timestamps_are_treated_as_utc():
    naive_from = (NOW - timedelta(days=1)).replace(tzinfo=None)
    naive_to = (NOW + timedelta(days=1)).replace(tzinfo=None)
    result = check_eligibility(
        policy(valid_from=naive_from, valid_to=naive_to), "MEM-1", now=NOW
    )
    assert result.eligible is True


def test_auto_adjudication_applies_coverage():
    eligibility = check_eligibility(policy(coverage_percent=80), "MEM-1", now=NOW)
    outcome = auto_adjudicate(round_money("125.50"), eligibility)
    assert outcome.status == ClaimStatus.APPROVED.value
    assert str(outcome.approved_amount) == "100.40"
    assert outcome.rejection_reason is None


def test_auto_adjudication_rejects_ineligible_policy():
    eligibility = check_eligibility(policy(active=False), "MEM-1", now=NOW)
    outcome = auto_adjudicate(round_money("125.50"), eligibility)
    assert outcome.status == ClaimStatus.REJECTED.value
    assert outcome.approved_amount is None
    assert "inactive" in outcome.rejection_reason


def test_approved_amount_rounds_to_cents():
    assert calculate_approved_amount(round_money("10.01"), 33) == round_money("3.30")


def test_claim_numbers_are_unique_and_prefixed():
    first = generate_claim_number(NOW)
    second = generate_claim_number(NOW)
    assert first.startswith("CLM-20260601-")
    assert first != second
