#!/usr/bin/env python3
"""Seed the Hospital Platform with sample dev/test data via api-gateway.

Loads departments.json, doctors.json, patients.json, and appointments.json
from this directory and POSTs them through the real REST APIs (going
through api-gateway, same as any other client), so the usual validation
in each service runs instead of writing rows straight into Postgres. This
matches the pattern already used for other seeding/bootstrap tasks in
this repo (services expose their own REST APIs; infrastructure/postgres/
init.sql only provisions one database per service, it does not insert
rows into them).

Usage:

    python3 tests/fixtures/seed-data/seed.py

Configuration (env vars, all optional):

    GATEWAY_URL   base URL of api-gateway, default http://localhost:8080
    SEED_TIMEOUT  per-request timeout in seconds, default 10

The script is idempotent-ish: entities are keyed on natural identifiers
(nationalId / licenseNumber / name) and a 409/422 on a record that
already exists is treated as "already seeded" rather than an error.
"""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

GATEWAY_URL = os.environ.get("GATEWAY_URL", "http://localhost:8080").rstrip("/")
TIMEOUT = float(os.environ.get("SEED_TIMEOUT", "10"))
HERE = Path(__file__).resolve().parent

ALREADY_SEEDED_CODES = {409, 422}
NOT_IMPLEMENTED_CODES = {404, 501}


def load(name: str) -> list[dict]:
    with open(HERE / name) as f:
        return json.load(f)


def post(path: str, payload: dict) -> tuple[int, dict | None]:
    url = f"{GATEWAY_URL}{path}"
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            body = resp.read()
            return resp.status, (json.loads(body) if body else None)
    except urllib.error.HTTPError as e:
        body = e.read()
        parsed = None
        try:
            parsed = json.loads(body) if body else None
        except json.JSONDecodeError:
            pass
        return e.code, parsed
    except urllib.error.URLError as e:
        print(f"  ! could not reach {url}: {e}", file=sys.stderr)
        return -1, None


def seed_collection(label: str, path: str, records: list[dict]) -> dict[str, str]:
    """POSTs each record, returns a map of a natural key -> created id for
    records that were created or already existed, so later collections
    can wire up foreign keys.
    """
    created: dict[str, str] = {}
    print(f"==> seeding {label} ({len(records)} records) via {path}")
    for record in records:
        natural_key = (
            record.get("nationalId")
            or record.get("licenseNumber")
            or record.get("name")
            or record.get("email")
        )
        status, body = post(path, record)
        if status == -1:
            print(f"  ! skipping {label}: gateway unreachable")
            return created
        if status in (200, 201):
            print(f"  + created {label}: {natural_key}")
            if body and "id" in body:
                created[natural_key] = body["id"]
        elif status in ALREADY_SEEDED_CODES:
            print(f"  = already seeded {label}: {natural_key}")
        elif status in NOT_IMPLEMENTED_CODES:
            print(f"  ~ {path} not implemented yet (HTTP {status}); skipping {label}")
        else:
            print(f"  ! unexpected HTTP {status} seeding {label} {natural_key}: {body}")
    return created


def main() -> int:
    departments = load("departments.json")
    doctors = load("doctors.json")
    patients = load("patients.json")
    appointments = load("appointments.json")

    seed_collection("departments", "/api/v1/departments", departments)
    doctor_ids = seed_collection("doctors", "/api/v1/doctors", doctors)
    patient_ids = seed_collection("patients", "/api/v1/patients", patients)

    print(f"==> seeding appointments ({len(appointments)} records) via /api/v1/appointments")
    for appt in appointments:
        patient_id = patient_ids.get(appt["patientNationalId"])
        doctor_id = doctor_ids.get(appt["doctorLicenseNumber"])
        if not patient_id or not doctor_id:
            print(
                f"  ~ skipping appointment {appt['patientNationalId']}/"
                f"{appt['doctorLicenseNumber']}: patient or doctor id not resolved "
                "(service may not be implemented yet, or record already existed "
                "under an id this run didn't see)"
            )
            continue
        payload = {
            "patientId": patient_id,
            "doctorId": doctor_id,
            "slotStart": appt["slotStart"],
            "slotEnd": appt["slotEnd"],
        }
        status, body = post("/api/v1/appointments", payload)
        if status in (200, 201):
            print(f"  + booked appointment for {appt['patientNationalId']}")
        elif status in ALREADY_SEEDED_CODES:
            print(f"  = slot already booked for {appt['patientNationalId']}")
        elif status in NOT_IMPLEMENTED_CODES:
            print(f"  ~ appointment-service booking not implemented yet (HTTP {status})")
        else:
            print(f"  ! unexpected HTTP {status} booking appointment: {body}")

    print("==> done")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
