from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import app


def test_no_show_route():
    with TestClient(app) as client:
        resp = client.post(
            "/api/v1/predictions/no-show",
            json={
                "appointment_id": "appt-1",
                "age": 50,
                "lead_days": 14,
                "prior_no_shows": 2,
                "distance_km": 10.0,
                "appointment_hour": 11,
                "day_of_week": 2,
                "has_insurance": False,
                "is_new_patient": True,
            },
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["appointment_id"] == "appt-1"
        assert 0.0 <= body["risk"] <= 1.0


def test_length_of_stay_route():
    with TestClient(app) as client:
        resp = client.post(
            "/api/v1/predictions/length-of-stay",
            json={
                "age": 70,
                "severity": 4,
                "department": "surgery",
                "diagnoses_count": 3,
                "is_inpatient": True,
            },
        )
        assert resp.status_code == 200
        assert resp.json()["predicted_days"] > 0


def test_optimal_slot_route():
    with TestClient(app) as client:
        resp = client.post(
            "/api/v1/predictions/optimal-slot",
            json={
                "age": 35,
                "lead_days": 3,
                "prior_no_shows": 0,
                "distance_km": 1.0,
                "has_insurance": True,
                "is_new_patient": False,
                "top_n": 2,
            },
        )
        assert resp.status_code == 200
        suggestions = resp.json()["suggestions"]
        assert len(suggestions) == 2


def test_cached_no_show_risk_missing_returns_404():
    with TestClient(app) as client:
        resp = client.get("/api/v1/predictions/no-show/unknown-appointment")
        assert resp.status_code == 404
