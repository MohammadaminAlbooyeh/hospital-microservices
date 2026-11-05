from __future__ import annotations

from app.domain.los_estimator import LengthOfStayEstimator
from app.domain.no_show_predictor import NoShowPredictor
from app.domain.slot_suggester import OptimalSlotSuggester


class _StubModel:
    """Minimal stand-in for a fitted sklearn estimator."""

    def __init__(self, proba: float = 0.5, value: float = 1.0) -> None:
        self._proba = proba
        self._value = value

    def predict_proba(self, x):
        return [[1 - self._proba, self._proba] for _ in x]

    def predict(self, x):
        return [self._value for _ in x]


NO_SHOW_SAMPLE = {
    "age": 30,
    "lead_days": 5,
    "prior_no_shows": 0,
    "distance_km": 2.0,
    "appointment_hour": 9,
    "day_of_week": 1,
    "has_insurance": True,
    "is_new_patient": False,
}

LOS_SAMPLE = {
    "age": 55,
    "severity": 3,
    "department": "cardiology",
    "diagnoses_count": 2,
    "is_inpatient": True,
}


def test_no_show_predictor_falls_back_to_heuristic_without_artifact():
    predictor = NoShowPredictor("does/not/exist.joblib")
    assert predictor.is_trained is False
    risk, model_used = predictor.predict(NO_SHOW_SAMPLE)
    assert 0.0 <= risk <= 1.0
    assert model_used == "noshow-heuristic"


def test_no_show_predictor_uses_loaded_artifact():
    artifact = {
        "model": _StubModel(proba=0.73),
        "feature_names": [
            "age",
            "lead_days",
            "prior_no_shows",
            "distance_km",
            "appointment_hour",
            "day_of_week",
            "has_insurance",
            "is_new_patient",
        ],
        "version": "1.0.0",
    }
    predictor = NoShowPredictor("unused.joblib", artifact=artifact)
    risk, model_used = predictor.predict(NO_SHOW_SAMPLE)
    assert risk == 0.73
    assert model_used == "noshow-rf-1.0.0"


def test_los_estimator_falls_back_to_heuristic_without_artifact():
    estimator = LengthOfStayEstimator("does/not/exist.joblib")
    assert estimator.is_trained is False
    days, model_used = estimator.predict(LOS_SAMPLE)
    assert days > 0
    assert model_used == "los-heuristic"


def test_los_estimator_uses_loaded_artifact():
    import numpy as np

    target_days = 4.0
    artifact = {
        "model": _StubModel(value=float(np.log1p(target_days))),
        "feature_names": ["age", "severity", "department_code", "diagnoses_count", "is_inpatient"],
        "version": "1.0.0",
    }
    estimator = LengthOfStayEstimator("unused.joblib", artifact=artifact)
    days, model_used = estimator.predict(LOS_SAMPLE)
    assert round(days, 1) == round(target_days, 1)
    assert model_used == "los-rf-1.0.0"


def test_optimal_slot_suggester_ranks_lowest_risk_first():
    predictor = NoShowPredictor("does/not/exist.joblib")
    suggester = OptimalSlotSuggester(predictor)
    suggestions = suggester.suggest(NO_SHOW_SAMPLE, top_n=3)
    assert len(suggestions) == 3
    risks = [s.predicted_risk for s in suggestions]
    assert risks == sorted(risks)
