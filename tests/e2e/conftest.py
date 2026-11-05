"""Shared fixtures for end-to-end tests.

These tests exercise the real HTTP surface of the platform through the
api-gateway, against a stack started with `docker compose up` (see the
root docker-compose.yml). They do not mock anything: every request goes
over the wire to whichever services implement the given endpoint.

Run against a running stack:

    make up            # from repo root, start the full platform
    pip install pytest httpx
    pytest tests/e2e -v

Configuration (all optional, read from the environment):

    GATEWAY_URL   base URL of api-gateway, default http://localhost:8080
    E2E_TIMEOUT   per-request timeout in seconds, default 10
"""
from __future__ import annotations

import os
import random
import string
import uuid

import httpx
import pytest

GATEWAY_URL = os.environ.get("GATEWAY_URL", "http://localhost:8080")
TIMEOUT = float(os.environ.get("E2E_TIMEOUT", "10"))


@pytest.fixture(scope="session")
def base_url() -> str:
    return GATEWAY_URL.rstrip("/")


@pytest.fixture(scope="session")
def client(base_url: str):
    with httpx.Client(base_url=base_url, timeout=TIMEOUT) as c:
        yield c


def random_suffix(n: int = 8) -> str:
    return "".join(random.choices(string.ascii_lowercase + string.digits, k=n))


def unique_email(prefix: str = "patient") -> str:
    return f"{prefix}.{random_suffix()}@e2e.hospital-platform.test"


def unique_national_id() -> str:
    return f"E2E-{uuid.uuid4().hex[:12].upper()}"


@pytest.fixture
def new_patient_payload() -> dict:
    """A valid PatientRequest body, matching patient-service's DTO
    (services-java/patient-service/src/main/java/com/hospital/patient/api/dto/PatientRequest.java).
    """
    return {
        "firstName": "Ada",
        "lastName": f"Lovelace-{random_suffix(4)}",
        "dateOfBirth": "1990-05-12",
        "gender": "FEMALE",
        "email": unique_email(),
        "phone": "+1-555-0100",
        "addressLine": "1 Analytical Engine Way",
        "city": "London",
        "country": "UK",
        "nationalId": unique_national_id(),
        "allergies": None,
        "emergencyContactName": "Charles Babbage",
        "emergencyContactPhone": "+1-555-0101",
    }
