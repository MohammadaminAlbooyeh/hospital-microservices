from __future__ import annotations

from datetime import date, datetime
from typing import Any

import pandas as pd
from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.v1.schemas import (
    AppointmentStatusResponse,
    AppointmentStatusUpdate,
    BedOccupancyResponse,
    NoShowResponse,
    OverviewResponse,
    RevenueByDepartmentItem,
    SeedResponse,
    WaitTimeItem,
)
from app.core.config import settings
from app.domain.aggregation import (
    bed_occupancy,
    no_show_overview,
    overview_kpis,
    revenue_by_department,
    wait_time_stats,
)
from app.domain.seed_data import build_seed_datasets
from app.domain.time_window import resolve_window
from app.infrastructure.db import AppointmentFact, EncounterFact, InvoiceFact, get_session
from hospital_shared.errors import AppError, NotFoundError

router = APIRouter(prefix="/api/v1/analytics", tags=["analytics"])

APPOINTMENT_FIELDS = (
    "appointment_id",
    "patient_id",
    "doctor_id",
    "department_id",
    "slot_start",
    "booked_at",
    "status",
)
ENCOUNTER_FIELDS = (
    "encounter_id",
    "patient_id",
    "department_id",
    "started_at",
    "completed_at",
    "wait_minutes",
)
INVOICE_FIELDS = (
    "invoice_id",
    "patient_id",
    "department_id",
    "total_amount",
    "patient_share",
    "insurer_share",
    "currency",
    "issued_at",
)


def _frame(rows: list[Any], fields: tuple[str, ...]) -> pd.DataFrame:
    return pd.DataFrame([{field: getattr(row, field) for field in fields} for row in rows])


def _window(from_date: date | None, to_date: date | None) -> tuple[datetime | None, datetime | None]:
    try:
        return resolve_window(from_date, to_date)
    except ValueError as exc:
        raise AppError(str(exc)) from exc


async def _appointments(
    session: AsyncSession, start: datetime | None, end: datetime | None
) -> pd.DataFrame:
    statement = select(AppointmentFact)
    if start is not None:
        statement = statement.where(AppointmentFact.slot_start >= start)
    if end is not None:
        statement = statement.where(AppointmentFact.slot_start <= end)
    rows = (await session.execute(statement)).scalars().all()
    return _frame(list(rows), APPOINTMENT_FIELDS)


async def _encounters(
    session: AsyncSession,
    start: datetime | None,
    end: datetime | None,
    *,
    active_only: bool = False,
) -> pd.DataFrame:
    statement = select(EncounterFact)
    if start is not None:
        statement = statement.where(EncounterFact.started_at >= start)
    if end is not None:
        statement = statement.where(EncounterFact.started_at <= end)
    if active_only:
        statement = statement.where(
            (EncounterFact.completed_at.is_(None)) | (EncounterFact.completed_at > datetime.now().astimezone())
        )
    rows = (await session.execute(statement)).scalars().all()
    return _frame(list(rows), ENCOUNTER_FIELDS)


async def _invoices(
    session: AsyncSession, start: datetime | None, end: datetime | None
) -> pd.DataFrame:
    statement = select(InvoiceFact)
    if start is not None:
        statement = statement.where(InvoiceFact.issued_at >= start)
    if end is not None:
        statement = statement.where(InvoiceFact.issued_at <= end)
    rows = (await session.execute(statement)).scalars().all()
    return _frame(list(rows), INVOICE_FIELDS)


@router.get("/overview", response_model=OverviewResponse)
async def get_overview(
    from_date: date | None = Query(default=None, alias="from"),
    to_date: date | None = Query(default=None, alias="to"),
    session: AsyncSession = Depends(get_session),
) -> OverviewResponse:
    start, end = _window(from_date, to_date)
    appointments = await _appointments(session, start, end)
    encounters = await _encounters(session, start, end)
    invoices = await _invoices(session, start, end)
    return OverviewResponse(**overview_kpis(appointments, encounters, invoices, settings.bed_capacity))


@router.get("/wait-times", response_model=list[WaitTimeItem])
async def get_wait_times(
    from_date: date | None = Query(default=None, alias="from"),
    to_date: date | None = Query(default=None, alias="to"),
    session: AsyncSession = Depends(get_session),
) -> list[WaitTimeItem]:
    start, end = _window(from_date, to_date)
    encounters = await _encounters(session, start, end)
    return [WaitTimeItem(**item) for item in wait_time_stats(encounters)]


@router.get("/no-show-rate", response_model=NoShowResponse)
async def get_no_show_rate(
    from_date: date | None = Query(default=None, alias="from"),
    to_date: date | None = Query(default=None, alias="to"),
    session: AsyncSession = Depends(get_session),
) -> NoShowResponse:
    start, end = _window(from_date, to_date)
    appointments = await _appointments(session, start, end)
    return NoShowResponse(**no_show_overview(appointments))


@router.get("/bed-occupancy", response_model=BedOccupancyResponse)
async def get_bed_occupancy(
    from_date: date | None = Query(default=None, alias="from"),
    to_date: date | None = Query(default=None, alias="to"),
    session: AsyncSession = Depends(get_session),
) -> BedOccupancyResponse:
    start, end = _window(from_date, to_date)
    encounters = await _encounters(session, start, end, active_only=True)
    return BedOccupancyResponse(**bed_occupancy(encounters, settings.bed_capacity))


@router.get("/revenue-by-department", response_model=list[RevenueByDepartmentItem])
async def get_revenue_by_department(
    from_date: date | None = Query(default=None, alias="from"),
    to_date: date | None = Query(default=None, alias="to"),
    session: AsyncSession = Depends(get_session),
) -> list[RevenueByDepartmentItem]:
    start, end = _window(from_date, to_date)
    invoices = await _invoices(session, start, end)
    return [RevenueByDepartmentItem(**item) for item in revenue_by_department(invoices)]


@router.post("/internal/appointments/{appointment_id}/status", response_model=AppointmentStatusResponse)
async def update_appointment_status(
    appointment_id: str,
    payload: AppointmentStatusUpdate,
    session: AsyncSession = Depends(get_session),
) -> AppointmentStatusResponse:
    facts = (
        await session.execute(
            select(AppointmentFact).where(AppointmentFact.appointment_id == appointment_id)
        )
    ).scalars().all()
    if not facts:
        raise NotFoundError(f"Appointment {appointment_id} not found")
    for fact in facts:
        fact.status = payload.status
    await session.commit()
    return AppointmentStatusResponse(appointment_id=appointment_id, status=payload.status, updated=len(facts))


@router.post("/internal/seed", response_model=SeedResponse)
async def seed_demo_data(session: AsyncSession = Depends(get_session)) -> SeedResponse:
    if not settings.analytics_seed_enabled:
        raise AppError(
            "Seeding is disabled; set ANALYTICS_SEED_ENABLED=true to enable it",
            status_code=403,
            code="SEED_DISABLED",
        )
    appointment_rows, encounter_rows, invoice_rows = build_seed_datasets()
    existing_appointments = set(
        (await session.execute(select(AppointmentFact.event_id))).scalars().all()
    )
    existing_encounters = set((await session.execute(select(EncounterFact.event_id))).scalars().all())
    existing_invoices = set((await session.execute(select(InvoiceFact.event_id))).scalars().all())

    seeded: list[str] = []
    skipped = 0
    for row in appointment_rows:
        if row["event_id"] in existing_appointments:
            skipped += 1
            continue
        session.add(AppointmentFact(**row))
        seeded.append(row["event_id"])
    for row in encounter_rows:
        if row["event_id"] in existing_encounters:
            skipped += 1
            continue
        session.add(EncounterFact(**row))
        seeded.append(row["event_id"])
    for row in invoice_rows:
        if row["event_id"] in existing_invoices:
            skipped += 1
            continue
        session.add(InvoiceFact(**row))
        seeded.append(row["event_id"])
    await session.commit()
    return SeedResponse(
        enabled=True,
        seeded_appointments=sum(1 for item in seeded if item.startswith("seed-appointment-")),
        seeded_encounters=sum(1 for item in seeded if "encounter" in item),
        seeded_invoices=sum(1 for item in seeded if item.startswith("seed-invoice-")),
        skipped_existing=skipped,
        event_ids=seeded,
    )
