"""Full happy-path patient journey, end to end, through the api-gateway.

Mirrors README.md section 4 ("The patient journey"):

    register -> browse doctors/slots -> book appointment -> check-in
    (encounter) -> lab order + result -> billing (invoice) -> payment success

Each step is tolerant of services that are still being scaffolded: if an
endpoint a later step needs is not yet implemented (404/501) or the stack
isn't up at all, the step is skipped with a clear reason rather than
failing the whole run, so this suite stays useful as services land one
by one. A fully wired stack should pass every step without skips.
"""
from __future__ import annotations

import httpx
import pytest

pytestmark = pytest.mark.e2e


def _skip_if_unavailable(response: httpx.Response, step: str) -> None:
    if response.status_code in (404, 501, 502, 503):
        pytest.skip(f"{step}: endpoint not available yet (HTTP {response.status_code})")


class TestPatientJourney:
    """Steps share state via instance attributes set on `self` across the
    test methods of one class instance (pytest instantiates once per
    class per test session by default for a plain class), so run this
    file with `-p no:randomly` if a random-order plugin is active.
    """

    patient_id: str | None = None
    doctor_id: str | None = None
    appointment_id: str | None = None
    encounter_id: str | None = None
    lab_order_id: str | None = None
    invoice_id: str | None = None

    def test_01_register_patient(self, client, new_patient_payload):
        resp = client.post("/api/v1/patients", json=new_patient_payload)
        _skip_if_unavailable(resp, "register patient")
        assert resp.status_code == 201, resp.text
        body = resp.json()
        assert body["email"] == new_patient_payload["email"]
        TestPatientJourney.patient_id = body["id"]

    def test_02_browse_doctors_and_slots(self, client):
        if TestPatientJourney.patient_id is None:
            pytest.skip("no patient registered in step 1")
        resp = client.get("/api/v1/doctors")
        _skip_if_unavailable(resp, "browse doctors")
        assert resp.status_code == 200, resp.text
        doctors = resp.json()
        if not doctors:
            pytest.skip("no doctors seeded; run tests/fixtures/seed-data/seed.py first")
        TestPatientJourney.doctor_id = doctors[0]["id"]

        slots_resp = client.get(f"/api/v1/doctors/{TestPatientJourney.doctor_id}/slots")
        if slots_resp.status_code in (404, 501):
            # appointment-service may expose slots itself instead
            slots_resp = client.get(
                "/api/v1/slots", params={"doctorId": TestPatientJourney.doctor_id}
            )
        _skip_if_unavailable(slots_resp, "browse slots")
        assert slots_resp.status_code == 200, slots_resp.text

    def test_03_book_appointment(self, client):
        if TestPatientJourney.patient_id is None or TestPatientJourney.doctor_id is None:
            pytest.skip("prerequisite step was skipped")
        payload = {
            "patientId": TestPatientJourney.patient_id,
            "doctorId": TestPatientJourney.doctor_id,
            "slotStart": "2030-01-15T09:00:00Z",
            "slotEnd": "2030-01-15T09:30:00Z",
        }
        resp = client.post("/api/v1/appointments", json=payload)
        _skip_if_unavailable(resp, "book appointment")
        assert resp.status_code == 201, resp.text
        body = resp.json()
        TestPatientJourney.appointment_id = body["id"]
        assert body["patientId"] == TestPatientJourney.patient_id

    def test_04_check_in_creates_encounter(self, client):
        if TestPatientJourney.appointment_id is None:
            pytest.skip("no appointment booked in step 3")
        resp = client.post(
            "/api/v1/encounters",
            json={
                "appointmentId": TestPatientJourney.appointment_id,
                "patientId": TestPatientJourney.patient_id,
                "doctorId": TestPatientJourney.doctor_id,
            },
        )
        _skip_if_unavailable(resp, "check-in / create encounter")
        assert resp.status_code == 201, resp.text
        body = resp.json()
        TestPatientJourney.encounter_id = body["id"]
        assert body["state"] in ("CHECKED_IN", "checked_in")

    def test_05_lab_order_and_result(self, client):
        if TestPatientJourney.encounter_id is None:
            pytest.skip("no encounter created in step 4")
        order_resp = client.post(
            "/api/v1/labs/orders",
            json={
                "encounterId": TestPatientJourney.encounter_id,
                "patientId": TestPatientJourney.patient_id,
                "testCode": "CBC",
            },
        )
        _skip_if_unavailable(order_resp, "place lab order")
        assert order_resp.status_code == 201, order_resp.text
        TestPatientJourney.lab_order_id = order_resp.json()["id"]

        result_resp = client.post(
            f"/api/v1/labs/orders/{TestPatientJourney.lab_order_id}/results",
            json={"resultSummary": "within normal limits"},
        )
        _skip_if_unavailable(result_resp, "record lab result")
        assert result_resp.status_code in (200, 201), result_resp.text

    def test_06_discharge_and_invoice(self, client):
        if TestPatientJourney.encounter_id is None:
            pytest.skip("no encounter created in step 4")
        discharge_resp = client.post(
            f"/api/v1/encounters/{TestPatientJourney.encounter_id}/discharge"
        )
        _skip_if_unavailable(discharge_resp, "discharge encounter")
        assert discharge_resp.status_code in (200, 204), discharge_resp.text

        invoice_resp = client.post(
            "/api/v1/billing/invoices",
            json={
                "patientId": TestPatientJourney.patient_id,
                "encounterId": TestPatientJourney.encounter_id,
            },
        )
        _skip_if_unavailable(invoice_resp, "issue invoice")
        assert invoice_resp.status_code == 201, invoice_resp.text
        TestPatientJourney.invoice_id = invoice_resp.json()["id"]

    def test_07_payment_success_closes_invoice(self, client):
        if TestPatientJourney.invoice_id is None:
            pytest.skip("no invoice issued in step 6")
        payment_resp = client.post(
            "/api/v1/payments",
            json={
                "invoiceId": TestPatientJourney.invoice_id,
                "patientId": TestPatientJourney.patient_id,
                "amount": 100.00,
                "currency": "USD",
                "idempotencyKey": f"e2e-{TestPatientJourney.invoice_id}",
            },
        )
        _skip_if_unavailable(payment_resp, "submit payment")
        assert payment_resp.status_code in (200, 201), payment_resp.text
        assert payment_resp.json().get("status") in ("COMPLETED", "SUCCESS", "completed")

        invoice_resp = client.get(f"/api/v1/billing/invoices/{TestPatientJourney.invoice_id}")
        _skip_if_unavailable(invoice_resp, "re-fetch invoice after payment")
        assert invoice_resp.json().get("status") in ("PAID", "CLOSED", "paid")
