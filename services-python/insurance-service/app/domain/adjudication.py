from __future__ import annotations

import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import Decimal, ROUND_HALF_UP
from enum import Enum

from app.domain.eligibility import EligibilityResult

MONEY = Decimal("0.01")


class ClaimStatus(str, Enum):
    SUBMITTED = "SUBMITTED"
    UNDER_REVIEW = "UNDER_REVIEW"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    PAID = "PAID"


@dataclass(frozen=True)
class AdjudicationResult:
    status: str
    approved_amount: Decimal | None
    rejection_reason: str | None


def round_money(value: Decimal | int | float | str) -> Decimal:
    return Decimal(str(value)).quantize(MONEY, rounding=ROUND_HALF_UP)


def calculate_approved_amount(amount: Decimal, coverage_percent: int) -> Decimal:
    return round_money(amount * Decimal(max(0, min(100, coverage_percent))) / Decimal(100))


def auto_adjudicate(amount: Decimal, eligibility: EligibilityResult) -> AdjudicationResult:
    """Apply a policy's coverage to a claim, or reject it with a reason."""
    if not eligibility.eligible:
        return AdjudicationResult(
            status=ClaimStatus.REJECTED.value,
            approved_amount=None,
            rejection_reason=eligibility.reason,
        )

    return AdjudicationResult(
        status=ClaimStatus.APPROVED.value,
        approved_amount=calculate_approved_amount(round_money(amount), eligibility.coverage_percent),
        rejection_reason=None,
    )


def generate_claim_number(now: datetime | None = None) -> str:
    stamp = (now or datetime.now(timezone.utc)).strftime("%Y%m%d")
    return f"CLM-{stamp}-{uuid.uuid4().hex[:10].upper()}"
