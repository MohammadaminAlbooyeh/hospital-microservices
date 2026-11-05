from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Protocol


class PolicyLike(Protocol):
    member_id: str
    active: bool
    valid_from: datetime
    valid_to: datetime
    coverage_percent: int


@dataclass(frozen=True)
class EligibilityResult:
    eligible: bool
    coverage_percent: int
    reason: str


def _as_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)


def check_eligibility(
    policy: PolicyLike | None,
    member_id: str,
    *,
    now: datetime | None = None,
) -> EligibilityResult:
    """Evaluate whether a member may use a policy at a point in time."""
    if policy is None:
        return EligibilityResult(False, 0, "policy not found")

    if policy.member_id != member_id:
        return EligibilityResult(False, 0, "member does not match policy")

    if not policy.active:
        return EligibilityResult(False, 0, "policy is inactive")

    current = _as_utc(now or datetime.now(timezone.utc))
    valid_from = _as_utc(policy.valid_from)
    valid_to = _as_utc(policy.valid_to)

    if current < valid_from:
        return EligibilityResult(False, 0, "policy is not yet effective")

    if current > valid_to:
        return EligibilityResult(False, 0, "policy has expired")

    return EligibilityResult(True, int(policy.coverage_percent), "eligible")
