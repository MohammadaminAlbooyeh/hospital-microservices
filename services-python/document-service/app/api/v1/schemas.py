from __future__ import annotations

from datetime import date, datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator


class InvoiceLine(BaseModel):
    model_config = ConfigDict(extra="ignore", str_strip_whitespace=True)

    description: str = Field(min_length=1, max_length=500)
    quantity: Decimal = Field(gt=0)
    unit_price: Decimal = Field(ge=0)


class InvoiceRequest(BaseModel):
    model_config = ConfigDict(extra="ignore", str_strip_whitespace=True)

    invoice_number: str = Field(min_length=1, max_length=128)
    patient_name: str = Field(min_length=1, max_length=200)
    patient_id: str = Field(min_length=1, max_length=64)
    issued_at: datetime | date
    currency: str = Field(default="USD", min_length=1, max_length=8)
    lines: list[InvoiceLine] = Field(min_length=1)
    insurer_share: Decimal = Field(default=Decimal("0"), ge=0)
    patient_share: Decimal = Field(default=Decimal("0"), ge=0)


class DischargeSummaryRequest(BaseModel):
    model_config = ConfigDict(extra="ignore", str_strip_whitespace=True)

    patient_name: str = Field(min_length=1, max_length=200)
    patient_id: str = Field(min_length=1, max_length=64)
    admission_date: date
    discharge_date: date
    diagnosis: str = Field(min_length=1, max_length=2000)
    medications: list[str] = Field(default_factory=list)
    follow_up_instructions: str = Field(min_length=1, max_length=4000)
    attending_physician: str | None = Field(default=None, max_length=200)
    encounter_id: str | None = Field(default=None, max_length=128)

    @model_validator(mode="after")
    def _validate_dates(self) -> "DischargeSummaryRequest":
        if self.discharge_date < self.admission_date:
            raise ValueError("discharge_date must not be before admission_date")
        return self


class MedicalCertificateRequest(BaseModel):
    model_config = ConfigDict(extra="ignore", str_strip_whitespace=True)

    patient_name: str = Field(min_length=1, max_length=200)
    patient_id: str = Field(min_length=1, max_length=64)
    diagnosis: str = Field(min_length=1, max_length=2000)
    rest_days: int = Field(ge=0, le=365)
    physician: str = Field(min_length=1, max_length=200)
    issue_date: date = Field(default_factory=date.today)
    start_date: date | None = None
    end_date: date | None = None


class DocumentResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    doc_type: str
    reference: str | None
    filename: str
    content_type: str
    size: int
    sha256: str
    created_at: datetime | None = None
    stored: bool = True
    download_url: str


class OcrResponse(BaseModel):
    filename: str
    content_type: str
    size: int
    text: str
    character_count: int
