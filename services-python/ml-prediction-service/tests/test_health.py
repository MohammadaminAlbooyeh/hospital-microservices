from fastapi.testclient import TestClient

from app.main import app


def test_health_and_prediction():
    client = TestClient(app)
    assert client.get('/health').status_code == 200
    resp = client.post('/api/v1/predictions/no-show', json={'age': 40, 'lead_days': 10, 'previous_no_shows': 1})
    assert resp.status_code == 200
    assert 'risk' in resp.json()
