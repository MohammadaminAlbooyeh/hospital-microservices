from __future__ import annotations

from collections.abc import Mapping, Sequence
from typing import Any

import numpy as np
import pandas as pd

NO_SHOW_FEATURES = [
    "age",
    "lead_days",
    "prior_no_shows",
    "distance_km",
    "appointment_hour",
    "day_of_week",
    "has_insurance",
    "is_new_patient",
]

LOS_FEATURES = [
    "age",
    "severity",
    "department_code",
    "diagnoses_count",
    "is_inpatient",
]

DEPARTMENT_CODES = {
    "cardiology": 0,
    "neurology": 1,
    "orthopedics": 2,
    "pediatrics": 3,
    "oncology": 4,
    "emergency": 5,
    "general": 6,
    "surgery": 7,
    "obstetrics": 8,
    "other": 9,
}

DEPARTMENT_ALIASES = {
    "general medicine": "general",
    "internal medicine": "general",
    "family medicine": "general",
    "er": "emergency",
    "a&e": "emergency",
    "accident and emergency": "emergency",
    "ob/gyn": "obstetrics",
    "obgyn": "obstetrics",
    "gynaecology": "obstetrics",
    "gynecology": "obstetrics",
    "orthopaedics": "orthopedics",
    "paediatrics": "pediatrics",
}

DAY_NAMES = {
    "monday": 0,
    "mon": 0,
    "tuesday": 1,
    "tue": 1,
    "tues": 1,
    "wednesday": 2,
    "wed": 2,
    "thursday": 3,
    "thu": 3,
    "thur": 3,
    "thurs": 3,
    "friday": 4,
    "fri": 4,
    "saturday": 5,
    "sat": 5,
    "sunday": 6,
    "sun": 6,
}


def normalise_day_of_week(value: int | str | None) -> int:
    if value is None:
        raise ValueError("day_of_week is required")
    if isinstance(value, str):
        key = value.strip().lower()
        if key in DAY_NAMES:
            return DAY_NAMES[key]
        try:
            numeric = int(key)
        except ValueError as exc:
            raise ValueError(f"Unsupported day_of_week value: {value!r}") from exc
        return _validate_day(numeric)
    return _validate_day(int(value))


def _validate_day(value: int) -> int:
    if not 0 <= value <= 6:
        raise ValueError("day_of_week must be between 0 (Monday) and 6 (Sunday)")
    return value


def normalise_department(value: str | None) -> str:
    key = (value or "other").strip().lower()
    key = DEPARTMENT_ALIASES.get(key, key)
    return key if key in DEPARTMENT_CODES else "other"


def department_code(value: str | None) -> int:
    return DEPARTMENT_CODES[normalise_department(value)]


def _as_bool(value: Any) -> float:
    if isinstance(value, str):
        return 1.0 if value.strip().lower() in {"true", "1", "yes", "y"} else 0.0
    return 1.0 if bool(value) else 0.0


def build_no_show_features(data: Mapping[str, Any]) -> dict[str, float]:
    hour = int(data["appointment_hour"])
    if not 0 <= hour <= 23:
        raise ValueError("appointment_hour must be between 0 and 23")
    age = float(data["age"])
    if not 0 <= age <= 130:
        raise ValueError("age must be between 0 and 130")
    lead_days = float(data["lead_days"])
    distance_km = float(data["distance_km"])
    prior_no_shows = float(data["prior_no_shows"])
    if lead_days < 0 or distance_km < 0 or prior_no_shows < 0:
        raise ValueError("lead_days, distance_km and prior_no_shows must be non-negative")
    return {
        "age": age,
        "lead_days": lead_days,
        "prior_no_shows": prior_no_shows,
        "distance_km": distance_km,
        "appointment_hour": float(hour),
        "day_of_week": float(normalise_day_of_week(data.get("day_of_week"))),
        "has_insurance": _as_bool(data.get("has_insurance", False)),
        "is_new_patient": _as_bool(data.get("is_new_patient", False)),
    }


def no_show_matrix(records: Sequence[Mapping[str, Any]]) -> pd.DataFrame:
    rows = [build_no_show_features(record) for record in records]
    return pd.DataFrame(rows, columns=NO_SHOW_FEATURES, dtype=float)


def build_los_features(data: Mapping[str, Any]) -> dict[str, float]:
    severity = int(data["severity"])
    if not 1 <= severity <= 5:
        raise ValueError("severity must be between 1 and 5")
    age = float(data["age"])
    if not 0 <= age <= 130:
        raise ValueError("age must be between 0 and 130")
    diagnoses = int(data["diagnoses_count"])
    if diagnoses < 0:
        raise ValueError("diagnoses_count must be non-negative")
    return {
        "age": age,
        "severity": float(severity),
        "department_code": float(department_code(str(data.get("department", "other")))),
        "diagnoses_count": float(diagnoses),
        "is_inpatient": _as_bool(data.get("is_inpatient", False)),
    }


def los_matrix(records: Sequence[Mapping[str, Any]]) -> pd.DataFrame:
    rows = [build_los_features(record) for record in records]
    return pd.DataFrame(rows, columns=LOS_FEATURES, dtype=float)


def generate_no_show_dataset(n_samples: int = 2400, random_state: int = 42) -> pd.DataFrame:
    rng = np.random.default_rng(random_state)
    age = rng.integers(18, 91, n_samples).astype(float)
    lead_days = rng.integers(0, 61, n_samples).astype(float)
    prior_no_shows = np.clip(rng.poisson(0.6, n_samples), 0, 8).astype(float)
    distance_km = np.clip(rng.gamma(2.0, 6.0, n_samples), 0, 60)
    appointment_hour = rng.integers(7, 19, n_samples).astype(float)
    day_of_week = rng.integers(0, 7, n_samples).astype(float)
    has_insurance = rng.integers(0, 2, n_samples).astype(float)
    is_new_patient = rng.integers(0, 2, n_samples).astype(float)

    logit = (
        -2.45
        + 0.045 * lead_days
        + 0.32 * prior_no_shows
        + 0.016 * distance_km
        + 0.55 * is_new_patient
        - 0.42 * has_insurance
        + 0.10 * np.maximum(0.0, age - 65.0) / 5.0
        + 0.20 * (day_of_week >= 5)
        + 0.012 * (appointment_hour - 13.0) ** 2
    )
    probability = 1.0 / (1.0 + np.exp(-logit))
    target = rng.binomial(1, probability).astype(int)

    frames = {
        "age": age,
        "lead_days": lead_days,
        "prior_no_shows": prior_no_shows,
        "distance_km": distance_km,
        "appointment_hour": appointment_hour,
        "day_of_week": day_of_week,
        "has_insurance": has_insurance,
        "is_new_patient": is_new_patient,
    }
    dataset = pd.DataFrame(frames)
    dataset["no_show"] = target
    return dataset


def generate_los_dataset(n_samples: int = 2400, random_state: int = 42) -> pd.DataFrame:
    rng = np.random.default_rng(random_state)
    age = rng.integers(1, 96, n_samples).astype(float)
    severity = rng.integers(1, 6, n_samples).astype(float)
    department_code_values = rng.integers(0, len(DEPARTMENT_CODES), n_samples).astype(float)
    diagnoses_count = np.clip(rng.poisson(1.8, n_samples), 0, 8).astype(float)
    inpatient_probability = 0.06 + 0.13 * severity
    is_inpatient = (rng.random(n_samples) < inpatient_probability).astype(float)

    days = (
        0.35
        + 0.55 * severity
        + 0.42 * diagnoses_count
        + 1.85 * is_inpatient
        + 0.16 * department_code_values
        + 0.012 * age
        + rng.normal(0.0, 0.55, n_samples)
    )
    days = np.clip(days, 0.2, 45.0)

    dataset = pd.DataFrame(
        {
            "age": age,
            "severity": severity,
            "department_code": department_code_values,
            "diagnoses_count": diagnoses_count,
            "is_inpatient": is_inpatient,
        }
    )
    dataset["length_of_stay_days"] = days
    return dataset
