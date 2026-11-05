from __future__ import annotations

import re
from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, model_validator


def _snake_case(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


class CamelModel(BaseModel):
    """Accepts both camelCase (Java producers) and snake_case (Python producers)."""

    model_config = ConfigDict(extra="allow", populate_by_name=True)

    @model_validator(mode="before")
    @classmethod
    def _normalise_keys(cls, data: Any) -> Any:
        if isinstance(data, dict):
            return {_snake_case(str(key)): value for key, value in data.items()}
        return data


class EventEnvelope(CamelModel):
    """Mirror of the Java EventEnvelope written by the transactional outbox."""

    event_id: str
    event_type: str
    occurred_at: datetime
    aggregate_id: str
    payload: dict[str, Any] = Field(default_factory=dict)
    correlation_id: str | None = None


class PatientRegistered(CamelModel):
    patient_id: str
    mrn: str | None = None
    first_name: str
    last_name: str
    email: str
    phone: str | None = None
    registered_at: datetime | None = None
    insurance_provider: str | None = None
    insurance_policy_number: str | None = None


class AppointmentBooked(CamelModel):
    appointment_id: str
    patient_id: str
    doctor_id: str
    department_id: str | None = None
    slot_start: datetime
    slot_end: datetime | None = None
    booked_at: datetime | None = None


class AppointmentCancelled(CamelModel):
    appointment_id: str
    patient_id: str
    reason: str | None = None
    cancelled_at: datetime | None = None


class EncounterCompleted(CamelModel):
    encounter_id: str
    patient_id: str
    doctor_id: str | None = None
    diagnoses: list[str] = Field(default_factory=list)
    completed_at: datetime | None = None


class LabResultReady(CamelModel):
    lab_order_id: str
    patient_id: str
    test_code: str
    result_summary: str | None = None
    critical: bool = False
    ready_at: datetime | None = None


class InvoiceIssued(CamelModel):
    invoice_id: str
    encounter_id: str | None = None
    patient_id: str
    total_amount: float
    patient_share: float
    insurer_share: float
    currency: str = "USD"
    issued_at: datetime | None = None


class PaymentCompleted(CamelModel):
    payment_id: str
    invoice_id: str
    patient_id: str | None = None
    amount: float
    currency: str = "USD"
    paid_at: datetime | None = None


class PaymentFailed(CamelModel):
    payment_id: str
    invoice_id: str
    patient_id: str | None = None
    amount: float
    reason: str | None = None
    failed_at: datetime | None = None


EVENT_MODELS: dict[str, type[BaseModel]] = {
    "PatientRegistered": PatientRegistered,
    "AppointmentBooked": AppointmentBooked,
    "AppointmentCancelled": AppointmentCancelled,
    "EncounterCompleted": EncounterCompleted,
    "LabResultReady": LabResultReady,
    "InvoiceIssued": InvoiceIssued,
    "PaymentCompleted": PaymentCompleted,
    "PaymentFailed": PaymentFailed,
}


def parse_event(raw: dict[str, Any]) -> tuple[EventEnvelope, BaseModel | None]:
    """Parse an envelope and its typed payload when the type is known."""
    envelope = EventEnvelope.model_validate(raw)
    model = EVENT_MODELS.get(envelope.event_type)
    payload = model.model_validate(envelope.payload) if model else None
    return envelope, payload
