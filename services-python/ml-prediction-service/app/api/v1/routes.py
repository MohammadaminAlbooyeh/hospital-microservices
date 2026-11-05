from __future__ import annotations

from pathlib import Path

from fastapi import APIRouter
from hospital_shared.errors import NotFoundError

from app.api.v1.schemas import (
    LengthOfStayRequest,
    LengthOfStayResponse,
    NoShowRequest,
    NoShowResponse,
    OptimalSlotRequest,
    OptimalSlotResponse,
    SlotSuggestionResponse,
)
from app.core.config import settings
from app.domain.los_estimator import LengthOfStayEstimator
from app.domain.no_show_predictor import NoShowPredictor
from app.domain.slot_suggester import OptimalSlotSuggester
from app.infrastructure.risk_cache import risk_cache

router = APIRouter(prefix="/api/v1/predictions", tags=["predictions"])

_model_dir = Path(settings.model_dir)
no_show_predictor = NoShowPredictor(_model_dir / settings.no_show_model_file)
los_estimator = LengthOfStayEstimator(_model_dir / settings.length_of_stay_model_file)
slot_suggester = OptimalSlotSuggester(no_show_predictor)


@router.post("/no-show", response_model=NoShowResponse)
async def predict_no_show(payload: NoShowRequest) -> NoShowResponse:
    risk, model_used = no_show_predictor.predict(payload.model_dump())
    return NoShowResponse(appointment_id=payload.appointment_id, risk=risk, model_used=model_used)


@router.get("/no-show/{appointment_id}", response_model=NoShowResponse)
async def get_cached_no_show_risk(appointment_id: str) -> NoShowResponse:
    cached = risk_cache.get(appointment_id)
    if cached is None:
        raise NotFoundError(f"No cached no-show risk for appointment {appointment_id}")
    return NoShowResponse(appointment_id=cached.appointment_id, risk=cached.risk, model_used=cached.model_used)


@router.post("/length-of-stay", response_model=LengthOfStayResponse)
async def predict_length_of_stay(payload: LengthOfStayRequest) -> LengthOfStayResponse:
    predicted_days, model_used = los_estimator.predict(payload.model_dump())
    return LengthOfStayResponse(predicted_days=predicted_days, model_used=model_used)


@router.post("/optimal-slot", response_model=OptimalSlotResponse)
async def suggest_optimal_slot(payload: OptimalSlotRequest) -> OptimalSlotResponse:
    data = payload.model_dump(exclude={"top_n"})
    suggestions = slot_suggester.suggest(data, top_n=payload.top_n)
    return OptimalSlotResponse(
        suggestions=[
            SlotSuggestionResponse(
                day_of_week=s.day_of_week,
                appointment_hour=s.appointment_hour,
                predicted_risk=s.predicted_risk,
            )
            for s in suggestions
        ],
        model_used="noshow-rf" if no_show_predictor.is_trained else "noshow-heuristic",
    )
