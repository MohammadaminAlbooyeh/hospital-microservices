from fastapi.testclient import TestClient

from app.main import app


def test_health_and_prediction():
    with TestClient(app) as client:
        assert client.get("/health").status_code == 200
        resp = client.post(
            "/api/v1/predictions/no-show",
            json={
                "age": 40,
                "lead_days": 10,
                "prior_no_shows": 1,
                "distance_km": 3.0,
                "appointment_hour": 10,
                "day_of_week": "monday",
                "has_insurance": True,
                "is_new_patient": False,
            },
        )
        assert resp.status_code == 200
        assert "risk" in resp.json()
