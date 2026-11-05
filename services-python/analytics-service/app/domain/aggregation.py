from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

import pandas as pd

STATUS_BOOKED = "BOOKED"
STATUS_CANCELLED = "CANCELLED"
STATUS_NO_SHOW = "NO_SHOW"
STATUS_COMPLETED = "COMPLETED"
VALID_STATUSES = {STATUS_BOOKED, STATUS_CANCELLED, STATUS_NO_SHOW, STATUS_COMPLETED}
ELIGIBLE_NO_SHOW_STATUSES = {STATUS_BOOKED, STATUS_COMPLETED, STATUS_NO_SHOW}
UNASSIGNED = "UNASSIGNED"


def _numeric(series: pd.Series) -> pd.Series:
    return pd.to_numeric(series, errors="coerce")


def _with_department(df: pd.DataFrame) -> pd.DataFrame:
    working = df.copy()
    working["department_id"] = (
        working["department_id"].where(working["department_id"].notna(), UNASSIGNED).astype(str)
    )
    return working


def wait_time_stats(encounters: pd.DataFrame) -> list[dict[str, Any]]:
    if encounters is None or encounters.empty:
        return []
    working = encounters.copy()
    if "wait_minutes" not in working.columns:
        return []
    if "department_id" not in working.columns:
        working["department_id"] = UNASSIGNED
    working["wait_minutes"] = _numeric(working["wait_minutes"])
    working = working.dropna(subset=["wait_minutes"])
    if working.empty:
        return []
    working = _with_department(working)
    grouped = (
        working.groupby("department_id", sort=True)["wait_minutes"]
        .agg(encounters="count", average_wait_minutes="mean", median_wait_minutes="median")
        .reset_index()
    )
    return [
        {
            "department_id": str(row.department_id),
            "encounters": int(row.encounters),
            "average_wait_minutes": round(float(row.average_wait_minutes), 2),
            "median_wait_minutes": round(float(row.median_wait_minutes), 2),
        }
        for row in grouped.itertuples(index=False)
    ]


def revenue_by_department(invoices: pd.DataFrame) -> list[dict[str, Any]]:
    if invoices is None or invoices.empty:
        return []
    working = invoices.copy()
    for column in ("total_amount", "patient_share", "insurer_share"):
        if column not in working.columns:
            working[column] = 0.0
        working[column] = _numeric(working[column]).fillna(0.0)
    if "department_id" not in working.columns:
        working["department_id"] = UNASSIGNED
    if "currency" not in working.columns:
        working["currency"] = "USD"
    if "invoice_id" not in working.columns:
        working["invoice_id"] = working.index.astype(str)
    working = _with_department(working)
    working["currency"] = working["currency"].where(working["currency"].notna(), "USD").astype(str).str.upper()
    grouped = working.groupby(["department_id", "currency"], sort=True).agg(
        invoice_count=("invoice_id", "count"),
        total_revenue=("total_amount", "sum"),
        patient_revenue=("patient_share", "sum"),
        insurer_revenue=("insurer_share", "sum"),
    )
    grouped = grouped.reset_index().sort_values(["total_revenue", "department_id"], ascending=[False, True])
    return [
        {
            "department_id": str(row.department_id),
            "currency": str(row.currency),
            "invoice_count": int(row.invoice_count),
            "total_revenue": round(float(row.total_revenue), 2),
            "patient_revenue": round(float(row.patient_revenue), 2),
            "insurer_revenue": round(float(row.insurer_revenue), 2),
        }
        for row in grouped.itertuples(index=False)
    ]


def no_show_overview(appointments: pd.DataFrame) -> dict[str, Any]:
    empty = {
        "total_appointments": 0,
        "eligible_appointments": 0,
        "booked_count": 0,
        "completed_count": 0,
        "cancelled_count": 0,
        "no_show_count": 0,
        "no_show_rate": 0.0,
        "by_department": [],
    }
    if appointments is None or appointments.empty or "status" not in appointments.columns:
        return empty

    working = appointments.copy()
    working["status"] = working["status"].astype(str).str.upper().str.strip()
    if "department_id" not in working.columns:
        working["department_id"] = UNASSIGNED
    working = _with_department(working)

    counts = working["status"].value_counts()
    eligible = working[working["status"].isin(ELIGIBLE_NO_SHOW_STATUSES)]
    no_show_count = int(counts.get(STATUS_NO_SHOW, 0))
    eligible_count = int(len(eligible))
    rate = round(no_show_count / eligible_count, 4) if eligible_count else 0.0

    by_department: list[dict[str, Any]] = []
    if eligible_count:
        grouped = eligible.groupby("department_id", sort=True)["status"].agg(
            total="count",
            no_show=lambda values: int((values == STATUS_NO_SHOW).sum()),
        )
        by_department = [
            {
                "department_id": str(row.Index),
                "total_appointments": int(row.total),
                "no_show_count": int(row.no_show),
                "no_show_rate": round(int(row.no_show) / int(row.total), 4) if int(row.total) else 0.0,
            }
            for row in grouped.itertuples()
        ]

    return {
        "total_appointments": int(len(working)),
        "eligible_appointments": eligible_count,
        "booked_count": int(counts.get(STATUS_BOOKED, 0)),
        "completed_count": int(counts.get(STATUS_COMPLETED, 0)),
        "cancelled_count": int(counts.get(STATUS_CANCELLED, 0)),
        "no_show_count": no_show_count,
        "no_show_rate": rate,
        "by_department": by_department,
    }


def bed_occupancy(
    encounters: pd.DataFrame,
    bed_capacity: int,
    as_of: datetime | None = None,
) -> dict[str, Any]:
    if bed_capacity <= 0:
        raise ValueError("bed capacity must be positive")
    moment = as_of or datetime.now(timezone.utc)
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=timezone.utc)
    occupied = 0
    if encounters is not None and not encounters.empty and "started_at" in encounters.columns:
        started = pd.to_datetime(encounters["started_at"], utc=True, errors="coerce")
        if "completed_at" in encounters.columns:
            completed = pd.to_datetime(encounters["completed_at"], utc=True, errors="coerce")
        else:
            completed = pd.Series(pd.NaT, index=encounters.index, dtype="datetime64[ns, UTC]")
        active = started.notna() & (started <= moment) & (completed.isna() | (completed > moment))
        occupied = int(active.sum())
    return {
        "occupied_beds": occupied,
        "total_beds": int(bed_capacity),
        "occupancy_rate": round(occupied / bed_capacity, 4),
        "as_of": moment,
    }


def wait_summary(encounters: pd.DataFrame) -> tuple[float | None, float | None]:
    if encounters is None or encounters.empty or "wait_minutes" not in encounters.columns:
        return None, None
    values = _numeric(encounters["wait_minutes"]).dropna()
    if values.empty:
        return None, None
    return round(float(values.mean()), 2), round(float(values.median()), 2)


def overview_kpis(
    appointments: pd.DataFrame,
    encounters: pd.DataFrame,
    invoices: pd.DataFrame,
    bed_capacity: int,
    as_of: datetime | None = None,
) -> dict[str, Any]:
    appointment_summary = no_show_overview(appointments)
    average_wait, median_wait = wait_summary(encounters)
    revenue = revenue_by_department(invoices)
    occupancy = bed_occupancy(encounters, bed_capacity, as_of=as_of)
    moment = occupancy["as_of"]

    return {
        "appointments_total": appointment_summary["total_appointments"],
        "eligible_appointments": appointment_summary["eligible_appointments"],
        "booked": appointment_summary["booked_count"],
        "cancelled": appointment_summary["cancelled_count"],
        "completed": appointment_summary["completed_count"],
        "no_show": appointment_summary["no_show_count"],
        "no_show_rate": appointment_summary["no_show_rate"],
        "encounters_total": int(len(encounters)) if encounters is not None else 0,
        "average_wait_minutes": average_wait,
        "median_wait_minutes": median_wait,
        "revenue_total": round(sum(item["total_revenue"] for item in revenue), 2),
        "patient_revenue_total": round(sum(item["patient_revenue"] for item in revenue), 2),
        "insurer_revenue_total": round(sum(item["insurer_revenue"] for item in revenue), 2),
        "occupied_beds": occupancy["occupied_beds"],
        "total_beds": occupancy["total_beds"],
        "occupancy_rate": occupancy["occupancy_rate"],
        "generated_at": moment,
    }
