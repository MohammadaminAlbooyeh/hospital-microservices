from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class WaitTimeItem(BaseModel):
    department_id: str
    encounters: int
    average_wait_minutes: float
    median_wait_minutes: float


class NoShowDepartmentItem(BaseModel):
    department_id: str
    total_appointments: int
    no_show_count: int
    no_show_rate: float


class NoShowResponse(BaseModel):
    total_appointments: int
    eligible_appointments: int
    booked_count: int
    completed_count: int
    cancelled_count: int
    no_show_count: int
    no_show_rate: float
    by_department: list[NoShowDepartmentItem]


class BedOccupancyResponse(BaseModel):
    occupied_beds: int
    total_beds: int
    occupancy_rate: float
    as_of: datetime


class RevenueByDepartmentItem(BaseModel):
    department_id: str
    currency: str
    invoice_count: int
    total_revenue: float
    patient_revenue: float
    insurer_revenue: float


class OverviewResponse(BaseModel):
    appointments_total: int
    eligible_appointments: int
    booked: int
    cancelled: int
    completed: int
    no_show: int
    no_show_rate: float
    encounters_total: int
    average_wait_minutes: float | None
    median_wait_minutes: float | None
    revenue_total: float
    patient_revenue_total: float
    insurer_revenue_total: float
    occupied_beds: int
    total_beds: int
    occupancy_rate: float
    generated_at: datetime


class AppointmentStatusUpdate(BaseModel):
    status: Literal["BOOKED", "CANCELLED", "NO_SHOW", "COMPLETED"]


class AppointmentStatusResponse(BaseModel):
    appointment_id: str
    status: str
    updated: int


class SeedResponse(BaseModel):
    enabled: bool
    seeded_appointments: int
    seeded_encounters: int
    seeded_invoices: int
    skipped_existing: int
    event_ids: list[str] = Field(default_factory=list)
