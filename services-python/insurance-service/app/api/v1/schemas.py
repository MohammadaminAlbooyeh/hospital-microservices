from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.domain.adjudication import ClaimStatus


class EligibilityRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    policy_number: str = Field(min_length=1, max_length=64)
    member_id: str = Field(min_length=1, max_length=64)


class EligibilityResponse(BaseModel):
    eligible: bool
    coverage_percent: int
    reason: str


class PolicyUpsertRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    policy_number: str = Field(min_length=1, max_length=64)
    provider: str = Field(min_length=1, max_length=128)
    member_id: str = Field(min_length=1, max_length=64)
    patient_id: str | None = Field(default=None, max_length=64)
    coverage_percent: int = Field(ge=0, le=100)
    valid_from: datetime
    valid_to: datetime
    active: bool = True

    @model_validator(mode="after")
    def validate_window(self) -> "PolicyUpsertRequest":
        if self.valid_to <= self.valid_from:
            raise ValueError("valid_to must be after valid_from")
        return self


class PolicyResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    policy_number: str
    provider: str
    member_id: str
    patient_id: str | None
    coverage_percent: int
    valid_from: datetime
    valid_to: datetime
    active: bool
    created_at: datetime
    updated_at: datetime


class ClaimCreateRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    policy_number: str = Field(min_length=1, max_length=64)
    patient_id: str = Field(min_length=1, max_length=64)
    encounter_id: str | None = Field(default=None, max_length=64)
    invoice_id: str | None = Field(default=None, max_length=64)
    amount: Decimal = Field(gt=0)


class ClaimResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    claim_number: str
    policy_number: str
    patient_id: str
    encounter_id: str | None
    invoice_id: str | None
    amount: Decimal
    status: str
    approved_amount: Decimal | None
    rejection_reason: str | None
    notes: str | None
    submitted_at: datetime
    updated_at: datetime


class AdjudicateRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    approved_amount: Decimal = Field(ge=0)
    notes: str | None = Field(default=None, max_length=2000)


class RejectRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    reason: str = Field(min_length=1, max_length=2000)


class ClaimStatusFilter(BaseModel):
    status: ClaimStatus | None = None
