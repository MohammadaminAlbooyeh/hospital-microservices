from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field


class NoShowRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    appointment_id: str | None = Field(default=None, max_length=64)
    age: int = Field(ge=0, le=130)
    lead_days: int = Field(ge=0, le=3650)
    prior_no_shows: int = Field(ge=0, le=100, default=0)
    distance_km: float = Field(ge=0, le=5000, default=5.0)
    appointment_hour: int = Field(ge=0, le=23)
    day_of_week: int | str = Field(default=0)
    has_insurance: bool = True
    is_new_patient: bool = False


class NoShowResponse(BaseModel):
    appointment_id: str | None = None
    risk: float
    model_used: str


class LengthOfStayRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    age: int = Field(ge=0, le=130)
    severity: int = Field(ge=1, le=5)
    department: str = "general"
    diagnoses_count: int = Field(ge=0, le=50, default=1)
    is_inpatient: bool = False


class LengthOfStayResponse(BaseModel):
    predicted_days: float
    model_used: str


class OptimalSlotRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    age: int = Field(ge=0, le=130)
    lead_days: int = Field(ge=0, le=3650, default=7)
    prior_no_shows: int = Field(ge=0, le=100, default=0)
    distance_km: float = Field(ge=0, le=5000, default=5.0)
    has_insurance: bool = True
    is_new_patient: bool = False
    top_n: int = Field(ge=1, le=10, default=3)


class SlotSuggestionResponse(BaseModel):
    day_of_week: int
    appointment_hour: int
    predicted_risk: float


class OptimalSlotResponse(BaseModel):
    suggestions: list[SlotSuggestionResponse]
    model_used: str
