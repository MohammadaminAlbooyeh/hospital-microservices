from __future__ import annotations

from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class Vitals(BaseModel):
    temperature_c: float | None = Field(default=None, ge=25, le=45)
    heart_rate: int | None = Field(default=None, ge=0, le=300)
    systolic_bp: int | None = Field(default=None, ge=0, le=300)
    diastolic_bp: int | None = Field(default=None, ge=0, le=200)
    respiratory_rate: int | None = Field(default=None, ge=0, le=100)
    spo2: int | None = Field(default=None, ge=0, le=100)
    pain_score: int = Field(default=0, ge=0, le=10)
    consciousness: Literal["alert", "voice", "pain", "unresponsive"] = "alert"


class TriageRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    patient_id: str = Field(min_length=1, max_length=64)
    encounter_id: str | None = Field(default=None, max_length=64)
    chief_complaint: str = Field(min_length=3, max_length=1000)
    age: int = Field(ge=0, le=130)
    pregnancy: bool = False
    vitals: Vitals


class TriageResponse(BaseModel):
    id: UUID
    patient_id: str
    encounter_id: str | None
    chief_complaint: str
    esi_level: int
    priority: str
    recommended_action: str
    rationale: list[str]
    model_used: str
    created_at: datetime | None = None
