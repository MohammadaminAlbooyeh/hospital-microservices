"""Concurrency test for appointment booking.

README.md section 5: "Optimistic locking in appointment-service. With
10,000 active patients, two people will hit the same slot in the same
millisecond. Use a JPA @Version column and surface a clean 409 to the
client."

This fires two booking requests for the *same* doctor+slot at the same
time (via a thread pool, so the requests are in flight concurrently) and
asserts exactly one succeeds (201) and the other is rejected with a
clean 409 Conflict - never two 201s, and never two patients silently
double-booked onto one slot.
"""
from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor

import httpx
import pytest

from conftest import unique_email, unique_national_id

pytestmark = pytest.mark.e2e

CONTESTED_SLOT_START = "2030-02-20T14:00:00Z"
CONTESTED_SLOT_END = "2030-02-20T14:30:00Z"


def _register_patient(client: httpx.Client, label: str) -> str | None:
    payload = {
        "firstName": "Concurrent",
        "lastName": f"Patient-{label}",
        "dateOfBirth": "1985-03-01",
        "gender": "OTHER",
        "email": unique_email(prefix=f"concurrent-{label}"),
        "phone": "+1-555-0200",
        "nationalId": unique_national_id(),
        "emergencyContactName": "N/A",
        "emergencyContactPhone": "+1-555-0201",
    }
    resp = client.post("/api/v1/patients", json=payload)
    if resp.status_code != 201:
        return None
    return resp.json()["id"]


def _get_any_doctor_id(client: httpx.Client) -> str | None:
    resp = client.get("/api/v1/doctors")
    if resp.status_code != 200:
        return None
    doctors = resp.json()
    return doctors[0]["id"] if doctors else None


def _book(client: httpx.Client, patient_id: str, doctor_id: str) -> httpx.Response:
    return client.post(
        "/api/v1/appointments",
        json={
            "patientId": patient_id,
            "doctorId": doctor_id,
            "slotStart": CONTESTED_SLOT_START,
            "slotEnd": CONTESTED_SLOT_END,
        },
    )


def test_two_simultaneous_bookings_for_same_slot_exactly_one_wins(client):
    doctor_id = _get_any_doctor_id(client)
    if doctor_id is None:
        pytest.skip("no doctors available; run tests/fixtures/seed-data/seed.py first")

    patient_a = _register_patient(client, "a")
    patient_b = _register_patient(client, "b")
    if patient_a is None or patient_b is None:
        pytest.skip("patient-service not available")

    # Warm up a connection to each patient's eventual request so the pool
    # has sockets ready, then fire both bookings as close to simultaneously
    # as the HTTP client allows.
    with ThreadPoolExecutor(max_workers=2) as pool:
        future_a = pool.submit(_book, client, patient_a, doctor_id)
        future_b = pool.submit(_book, client, patient_b, doctor_id)
        resp_a = future_a.result()
        resp_b = future_b.result()

    if resp_a.status_code in (404, 501, 502, 503) and resp_b.status_code in (404, 501, 502, 503):
        pytest.skip("appointment-service booking endpoint not available yet")

    statuses = sorted([resp_a.status_code, resp_b.status_code])

    # Exactly one booking must succeed and the other must be rejected with
    # a clean 409 Conflict surfaced from the optimistic-locking @Version
    # check - never both succeeding (double-booked slot) and never both
    # failing.
    assert statuses == [201, 409], (
        f"expected exactly one 201 and one 409, got {resp_a.status_code} and "
        f"{resp_b.status_code}. Bodies: {resp_a.text!r}, {resp_b.text!r}"
    )

    winner = resp_a if resp_a.status_code == 201 else resp_b
    winner_body = winner.json()
    assert winner_body["doctorId"] == doctor_id

    # A second, sequential attempt against the now-booked slot must also
    # be rejected - the lock isn't a one-shot race artifact, the slot
    # stays unavailable.
    patient_c = _register_patient(client, "c")
    if patient_c is not None:
        resp_c = _book(client, patient_c, doctor_id)
        assert resp_c.status_code == 409, resp_c.text
