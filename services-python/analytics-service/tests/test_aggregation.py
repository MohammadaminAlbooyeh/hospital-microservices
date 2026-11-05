from __future__ import annotations

from datetime import date, datetime, timedelta, timezone

import pandas as pd
import pytest

from app.domain.aggregation import (
    bed_occupancy,
    no_show_overview,
    overview_kpis,
    revenue_by_department,
    wait_time_stats,
)
from app.domain.seed_data import build_seed_datasets
from app.domain.time_window import resolve_window


def test_wait_time_stats_returns_mean_and_median_per_department():
    encounters = pd.DataFrame(
        [
            {"department_id": "cardiology", "wait_minutes": 10.0},
            {"department_id": "cardiology", "wait_minutes": 30.0},
            {"department_id": "neurology", "wait_minutes": 20.0},
            {"department_id": None, "wait_minutes": 40.0},
        ]
    )
    result = wait_time_stats(encounters)
    by_department = {item["department_id"]: item for item in result}
    assert by_department["cardiology"]["encounters"] == 2
    assert by_department["cardiology"]["average_wait_minutes"] == 20.0
    assert by_department["cardiology"]["median_wait_minutes"] == 20.0
    assert by_department["neurology"]["average_wait_minutes"] == 20.0
    assert by_department["UNASSIGNED"]["encounters"] == 1


def test_wait_time_stats_ignores_missing_and_invalid_values():
    encounters = pd.DataFrame(
        [
            {"department_id": "cardiology", "wait_minutes": 10.0},
            {"department_id": "cardiology", "wait_minutes": None},
            {"department_id": "cardiology", "wait_minutes": "not-a-number"},
        ]
    )
    result = wait_time_stats(encounters)
    assert len(result) == 1
    assert result[0]["encounters"] == 1
    assert result[0]["average_wait_minutes"] == 10.0


def test_wait_time_stats_empty_frame():
    assert wait_time_stats(pd.DataFrame()) == []


def test_revenue_by_department_groups_sums_and_currency():
    invoices = pd.DataFrame(
        [
            {
                "invoice_id": "i1",
                "department_id": "cardiology",
                "total_amount": 1000.0,
                "patient_share": 200.0,
                "insurer_share": 800.0,
                "currency": "usd",
            },
            {
                "invoice_id": "i2",
                "department_id": "cardiology",
                "total_amount": 500.0,
                "patient_share": 100.0,
                "insurer_share": 400.0,
                "currency": "USD",
            },
            {
                "invoice_id": "i3",
                "department_id": None,
                "total_amount": 250.0,
                "patient_share": 250.0,
                "insurer_share": 0.0,
                "currency": "USD",
            },
        ]
    )
    result = revenue_by_department(invoices)
    by_department = {item["department_id"]: item for item in result}
    assert by_department["cardiology"]["invoice_count"] == 2
    assert by_department["cardiology"]["total_revenue"] == 1500.0
    assert by_department["cardiology"]["patient_revenue"] == 300.0
    assert by_department["cardiology"]["insurer_revenue"] == 1200.0
    assert by_department["cardiology"]["currency"] == "USD"
    assert by_department["UNASSIGNED"]["total_revenue"] == 250.0
    assert result[0]["department_id"] == "cardiology"


def test_no_show_rate_excludes_cancelled_and_groups_by_department():
    appointments = pd.DataFrame(
        [
            {"department_id": "cardiology", "status": "NO_SHOW"},
            {"department_id": "cardiology", "status": "BOOKED"},
            {"department_id": "cardiology", "status": "CANCELLED"},
            {"department_id": "neurology", "status": "NO_SHOW"},
            {"department_id": "neurology", "status": "COMPLETED"},
            {"department_id": "neurology", "status": "COMPLETED"},
        ]
    )
    result = no_show_overview(appointments)
    assert result["total_appointments"] == 6
    assert result["eligible_appointments"] == 5
    assert result["cancelled_count"] == 1
    assert result["no_show_count"] == 2
    assert result["no_show_rate"] == pytest.approx(0.4)
    by_department = {item["department_id"]: item for item in result["by_department"]}
    assert by_department["cardiology"]["no_show_rate"] == pytest.approx(0.5)
    assert by_department["neurology"]["no_show_rate"] == pytest.approx(1 / 3, rel=1e-3)


def test_no_show_rate_empty_safe():
    result = no_show_overview(pd.DataFrame())
    assert result["no_show_rate"] == 0.0
    assert result["total_appointments"] == 0


def test_bed_occupancy_counts_active_encounters_only():
    now = datetime(2026, 5, 1, 12, 0, tzinfo=timezone.utc)
    encounters = pd.DataFrame(
        [
            {"started_at": now - timedelta(hours=4), "completed_at": None},
            {"started_at": now - timedelta(hours=6), "completed_at": now - timedelta(hours=1)},
            {"started_at": now - timedelta(hours=2), "completed_at": now + timedelta(hours=2)},
            {"started_at": now + timedelta(hours=1), "completed_at": None},
        ]
    )
    result = bed_occupancy(encounters, bed_capacity=10, as_of=now)
    assert result["occupied_beds"] == 2
    assert result["total_beds"] == 10
    assert result["occupancy_rate"] == 0.2


def test_overview_kpis_combines_all_facts():
    now = datetime(2026, 5, 1, 12, 0, tzinfo=timezone.utc)
    appointments = pd.DataFrame(
        [
            {"status": "COMPLETED", "department_id": "cardiology"},
            {"status": "NO_SHOW", "department_id": "cardiology"},
        ]
    )
    encounters = pd.DataFrame(
        [
            {
                "started_at": now - timedelta(hours=2),
                "completed_at": now - timedelta(hours=1),
                "wait_minutes": 15.0,
                "department_id": "cardiology",
            }
        ]
    )
    invoices = pd.DataFrame(
        [
            {
                "invoice_id": "i1",
                "department_id": "cardiology",
                "total_amount": 100.0,
                "patient_share": 20.0,
                "insurer_share": 80.0,
                "currency": "USD",
            }
        ]
    )
    result = overview_kpis(appointments, encounters, invoices, bed_capacity=20, as_of=now)
    assert result["appointments_total"] == 2
    assert result["no_show_rate"] == 0.5
    assert result["average_wait_minutes"] == 15.0
    assert result["revenue_total"] == 100.0
    assert result["occupied_beds"] == 0
    assert result["total_beds"] == 20


def test_resolve_window_builds_inclusive_bounds_and_validates_order():
    start, end = resolve_window(date(2026, 1, 1), date(2026, 1, 31))
    assert start == datetime(2026, 1, 1, tzinfo=timezone.utc)
    assert end and end.date() == date(2026, 1, 31)
    assert resolve_window(None, None) == (None, None)
    with pytest.raises(ValueError):
        resolve_window(date(2026, 2, 1), date(2026, 1, 1))


def test_seed_datasets_are_deterministic_and_include_no_shows():
    first = build_seed_datasets(datetime(2026, 5, 1, tzinfo=timezone.utc))
    second = build_seed_datasets(datetime(2026, 5, 1, tzinfo=timezone.utc))
    assert first == second
    appointments, encounters, invoices = first
    assert len(appointments) == 30
    assert any(item["status"] == "NO_SHOW" for item in appointments)
    assert encounters and invoices
    assert len({item["event_id"] for item in appointments}) == len(appointments)
