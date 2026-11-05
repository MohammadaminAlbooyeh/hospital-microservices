from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Any

DEPARTMENTS = ("cardiology", "neurology", "orthopedics", "pediatrics")
STATUSES = ("BOOKED", "COMPLETED", "CANCELLED", "NO_SHOW")


def _status_for(index: int) -> str:
    if index % 11 == 0:
        return "NO_SHOW"
    if index % 9 == 0:
        return "CANCELLED"
    if index % 3 == 0:
        return "COMPLETED"
    return "BOOKED"


def build_seed_datasets(now: datetime | None = None) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]]]:
    moment = now or datetime.now(timezone.utc)
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=timezone.utc)

    appointments: list[dict[str, Any]] = []
    encounters: list[dict[str, Any]] = []
    invoices: list[dict[str, Any]] = []

    for index in range(30):
        department = DEPARTMENTS[index % len(DEPARTMENTS)]
        slot_start = moment - timedelta(days=index % 45) + timedelta(hours=8 + (index % 9))
        status = _status_for(index)
        appointments.append(
            {
                "event_id": f"seed-appointment-{index:03d}",
                "appointment_id": f"seed-appt-{index:03d}",
                "patient_id": f"seed-patient-{index % 12:03d}",
                "doctor_id": f"seed-doctor-{index % 6:03d}",
                "department_id": department,
                "slot_start": slot_start,
                "booked_at": slot_start - timedelta(days=10),
                "status": status,
            }
        )

        if index < 20 and status in {"BOOKED", "COMPLETED"}:
            wait_minutes = float(10 + (index * 7) % 70)
            completed_at = slot_start + timedelta(hours=1, minutes=int(wait_minutes))
            encounters.append(
                {
                    "event_id": f"seed-encounter-{index:03d}",
                    "encounter_id": f"seed-enc-{index:03d}",
                    "patient_id": f"seed-patient-{index % 12:03d}",
                    "department_id": department,
                    "started_at": slot_start,
                    "completed_at": completed_at,
                    "wait_minutes": wait_minutes,
                }
            )

        if status == "COMPLETED" or index in {0, 11}:
            total = 500.0 + index * 137.5
            invoices.append(
                {
                    "event_id": f"seed-invoice-{index:03d}",
                    "invoice_id": f"seed-inv-{index:03d}",
                    "patient_id": f"seed-patient-{index % 12:03d}",
                    "department_id": None if index % 7 == 0 else department,
                    "total_amount": total,
                    "patient_share": round(total * 0.2, 2),
                    "insurer_share": round(total * 0.8, 2),
                    "currency": "USD",
                    "issued_at": slot_start + timedelta(hours=2),
                }
            )

    for index in range(4):
        started_at = moment - timedelta(days=index, hours=3)
        encounters.append(
            {
                "event_id": f"seed-active-encounter-{index:03d}",
                "encounter_id": f"seed-active-enc-{index:03d}",
                "patient_id": f"seed-inpatient-{index:03d}",
                "department_id": DEPARTMENTS[index % len(DEPARTMENTS)],
                "started_at": started_at,
                "completed_at": None,
                "wait_minutes": 0.0,
            }
        )

    return appointments, encounters, invoices
