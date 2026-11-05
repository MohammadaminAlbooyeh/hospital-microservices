from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.v1.schemas import TriageRequest, TriageResponse
from app.core.config import settings
from app.domain.esi_calculator import EsiCalculator
from app.infrastructure.db import TriageRecord, get_session
from hospital_shared.errors import NotFoundError

router = APIRouter(prefix="/api/v1/triage", tags=["triage"])
calculator = EsiCalculator(settings.model_path)


def _to_response(record: TriageRecord) -> TriageResponse:
    return TriageResponse(
        id=record.id,
        patient_id=record.patient_id,
        encounter_id=record.encounter_id,
        chief_complaint=record.chief_complaint,
        esi_level=record.esi_level,
        priority=record.priority,
        recommended_action=record.recommended_action,
        rationale=record.rationale,
        model_used=record.model_used,
        created_at=record.created_at,
    )


@router.post("/assess", response_model=TriageResponse, status_code=status.HTTP_201_CREATED)
async def assess_triage(payload: TriageRequest, session: AsyncSession = Depends(get_session)) -> TriageResponse:
    outcome = calculator.calculate(
        age=payload.age,
        chief_complaint=payload.chief_complaint,
        vitals=payload.vitals,
        pregnancy=payload.pregnancy,
    )
    record = TriageRecord(
        patient_id=payload.patient_id,
        encounter_id=payload.encounter_id,
        chief_complaint=payload.chief_complaint,
        esi_level=outcome.esi_level,
        priority=outcome.priority,
        recommended_action=outcome.recommended_action,
        vitals=payload.vitals.model_dump(),
        rationale=outcome.rationale,
        model_used=outcome.model_used,
    )
    session.add(record)
    await session.commit()
    await session.refresh(record)
    return _to_response(record)


@router.get("/{record_id}", response_model=TriageResponse)
async def get_triage(record_id: UUID, session: AsyncSession = Depends(get_session)) -> TriageResponse:
    record = await session.get(TriageRecord, record_id)
    if record is None:
        raise NotFoundError(f"Triage record {record_id} not found")
    return _to_response(record)


@router.get("/patients/{patient_id}", response_model=list[TriageResponse])
async def list_patient_triage(patient_id: str, session: AsyncSession = Depends(get_session)):
    records = (
        await session.execute(
            select(TriageRecord)
            .where(TriageRecord.patient_id == patient_id)
            .order_by(TriageRecord.created_at.desc())
        )
    ).scalars().all()
    return [_to_response(record) for record in records]
