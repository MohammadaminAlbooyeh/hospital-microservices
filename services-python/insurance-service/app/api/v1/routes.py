from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
from uuid import UUID

from fastapi import APIRouter, Depends, Query, Response
from fastapi import status as http_status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.v1.schemas import (
    AdjudicateRequest,
    ClaimCreateRequest,
    ClaimResponse,
    EligibilityRequest,
    EligibilityResponse,
    PolicyResponse,
    PolicyUpsertRequest,
    RejectRequest,
)
from app.domain.adjudication import ClaimStatus, auto_adjudicate, generate_claim_number, round_money
from app.domain.eligibility import check_eligibility
from app.infrastructure.db import Claim, Policy, get_session
from hospital_shared.errors import AppError, ConflictError, NotFoundError

router = APIRouter(prefix="/api/v1/insurance", tags=["insurance"])

_EDITABLE_POLICY_FIELDS = (
    "provider",
    "member_id",
    "patient_id",
    "coverage_percent",
    "valid_from",
    "valid_to",
    "active",
)


@router.post("/eligibility", response_model=EligibilityResponse)
async def check_policy_eligibility(
    payload: EligibilityRequest,
    session: AsyncSession = Depends(get_session),
) -> EligibilityResponse:
    policy = await session.scalar(select(Policy).where(Policy.policy_number == payload.policy_number))
    result = check_eligibility(policy, payload.member_id)
    return EligibilityResponse(
        eligible=result.eligible,
        coverage_percent=result.coverage_percent,
        reason=result.reason,
    )


@router.post("/policies", response_model=PolicyResponse)
async def upsert_policy(
    payload: PolicyUpsertRequest,
    response: Response,
    session: AsyncSession = Depends(get_session),
) -> Policy:
    policy = await session.scalar(select(Policy).where(Policy.policy_number == payload.policy_number))
    if policy is None:
        policy = Policy(policy_number=payload.policy_number, **payload.model_dump(exclude={"policy_number"}))
        session.add(policy)
        response.status_code = http_status.HTTP_201_CREATED
    else:
        for field in _EDITABLE_POLICY_FIELDS:
            setattr(policy, field, getattr(payload, field))
        response.status_code = http_status.HTTP_200_OK

    await session.commit()
    await session.refresh(policy)
    return policy


@router.get("/policies/{policy_number}", response_model=PolicyResponse)
async def get_policy(policy_number: str, session: AsyncSession = Depends(get_session)) -> Policy:
    policy = await session.scalar(select(Policy).where(Policy.policy_number == policy_number))
    if policy is None:
        raise NotFoundError(f"Policy {policy_number} not found")
    return policy


@router.post("/claims", response_model=ClaimResponse, status_code=http_status.HTTP_201_CREATED)
async def create_claim(
    payload: ClaimCreateRequest,
    session: AsyncSession = Depends(get_session),
) -> Claim:
    policy = await session.scalar(select(Policy).where(Policy.policy_number == payload.policy_number))
    eligibility = check_eligibility(policy, policy.member_id if policy else "")
    outcome = auto_adjudicate(round_money(payload.amount), eligibility)
    now = datetime.now(timezone.utc)

    claim = Claim(
        claim_number=generate_claim_number(now),
        policy_number=payload.policy_number,
        patient_id=payload.patient_id,
        encounter_id=payload.encounter_id,
        invoice_id=payload.invoice_id,
        amount=round_money(payload.amount),
        status=ClaimStatus.SUBMITTED.value,
        submitted_at=now,
        updated_at=now,
    )
    claim.status = outcome.status
    claim.approved_amount = outcome.approved_amount
    claim.rejection_reason = outcome.rejection_reason

    session.add(claim)
    await session.commit()
    await session.refresh(claim)
    return claim


@router.get("/claims", response_model=list[ClaimResponse])
async def list_claims(
    patient_id: str | None = Query(default=None, max_length=64),
    claim_status: ClaimStatus | None = Query(default=None, alias="status"),
    limit: int = Query(default=100, ge=1, le=500),
    session: AsyncSession = Depends(get_session),
) -> list[Claim]:
    statement = select(Claim).order_by(Claim.submitted_at.desc()).limit(limit)
    if patient_id:
        statement = statement.where(Claim.patient_id == patient_id)
    if claim_status:
        statement = statement.where(Claim.status == claim_status.value)
    return list((await session.scalars(statement)).all())


@router.get("/claims/{claim_id}", response_model=ClaimResponse)
async def get_claim(claim_id: UUID, session: AsyncSession = Depends(get_session)) -> Claim:
    claim = await session.get(Claim, claim_id)
    if claim is None:
        raise NotFoundError(f"Claim {claim_id} not found")
    return claim


async def _load_claim(claim_id: UUID, session: AsyncSession) -> Claim:
    claim = await session.get(Claim, claim_id)
    if claim is None:
        raise NotFoundError(f"Claim {claim_id} not found")
    if claim.status == ClaimStatus.PAID.value:
        raise ConflictError(f"Claim {claim.claim_number} has already been paid")
    return claim


@router.post("/claims/{claim_id}/adjudicate", response_model=ClaimResponse)
async def adjudicate_claim(
    claim_id: UUID,
    payload: AdjudicateRequest,
    session: AsyncSession = Depends(get_session),
) -> Claim:
    claim = await _load_claim(claim_id, session)
    approved = round_money(payload.approved_amount)
    if approved > Decimal(claim.amount):
        raise AppError("approved_amount cannot exceed the claim amount", code="INVALID_AMOUNT")

    claim.status = ClaimStatus.APPROVED.value
    claim.approved_amount = approved
    claim.rejection_reason = None
    claim.notes = payload.notes
    claim.updated_at = datetime.now(timezone.utc)
    await session.commit()
    await session.refresh(claim)
    return claim


@router.post("/claims/{claim_id}/reject", response_model=ClaimResponse)
async def reject_claim(
    claim_id: UUID,
    payload: RejectRequest,
    session: AsyncSession = Depends(get_session),
) -> Claim:
    claim = await _load_claim(claim_id, session)
    claim.status = ClaimStatus.REJECTED.value
    claim.approved_amount = None
    claim.rejection_reason = payload.reason
    claim.updated_at = datetime.now(timezone.utc)
    await session.commit()
    await session.refresh(claim)
    return claim


@router.post("/claims/{claim_id}/pay", response_model=ClaimResponse)
async def pay_claim(claim_id: UUID, session: AsyncSession = Depends(get_session)) -> Claim:
    claim = await session.get(Claim, claim_id)
    if claim is None:
        raise NotFoundError(f"Claim {claim_id} not found")
    if claim.status == ClaimStatus.PAID.value:
        return claim
    if claim.status != ClaimStatus.APPROVED.value:
        raise ConflictError(f"Claim {claim.claim_number} is not approved and cannot be paid")

    claim.status = ClaimStatus.PAID.value
    claim.updated_at = datetime.now(timezone.utc)
    await session.commit()
    await session.refresh(claim)
    return claim
