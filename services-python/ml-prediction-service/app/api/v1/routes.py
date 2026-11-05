from __future__ import annotations

from fastapi import APIRouter
from pydantic import BaseModel, Field

router = APIRouter(prefix="/api/v1/predictions", tags=["predictions"])


class NoShowRequest(BaseModel):
    age: int = Field(ge=0, le=130)
    lead_days: int = Field(ge=0, le=3650)
    previous_no_shows: int = Field(ge=0, le=100, default=0)


@router.post("/no-show")
async def predict_no_show(payload: NoShowRequest) -> dict[str, float | str]:
    # Training scripts exist under app/training; this runtime endpoint is a safe placeholder.
    risk = min(0.99, 0.05 + payload.lead_days * 0.002 + payload.previous_no_shows * 0.08)
    return {"model": "noshow-placeholder", "risk": round(risk, 4)}
