from fastapi.testclient import TestClient

from app.main import app


def test_health_and_record():
    client = TestClient(app)
    assert client.get('/health').status_code == 200
    resp = client.post('/api/v1/audit/events', json={
        'actor': 'user-1', 'action': 'READ', 'resource_type': 'patient', 'resource_id': 'p-1'
    })
    assert resp.status_code == 200
    assert client.get('/api/v1/audit/events').json()[0]['resource_id'] == 'p-1'
