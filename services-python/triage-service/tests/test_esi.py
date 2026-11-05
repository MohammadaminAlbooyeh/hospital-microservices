from app.domain.esi_calculator import EsiCalculator
from app.domain.triage_rules import evaluate


class Vitals:
    def __init__(self, **kwargs):
        self.temperature_c = kwargs.get("temperature_c", 36.8)
        self.heart_rate = kwargs.get("heart_rate", 78)
        self.systolic_bp = kwargs.get("systolic_bp", 120)
        self.diastolic_bp = kwargs.get("diastolic_bp", 78)
        self.respiratory_rate = kwargs.get("respiratory_rate", 16)
        self.spo2 = kwargs.get("spo2", 98)
        self.pain_score = kwargs.get("pain_score", 0)
        self.consciousness = kwargs.get("consciousness", "alert")


def test_unresponsive_is_esi_1():
    result = evaluate(Vitals(consciousness="unresponsive"), age=40)
    assert result.level == 1


def test_critical_hypoxaemia_is_esi_1():
    result = evaluate(Vitals(spo2=80), age=55)
    assert result.level == 1


def test_chest_pain_is_high_acuity():
    result = evaluate(Vitals(), age=60, chief_complaint="crushing chest pain")
    assert result.level <= 2


def test_stable_patient_is_low_acuity():
    result = evaluate(Vitals(), age=30, chief_complaint="mild rash")
    assert result.level >= 4


def test_calculator_returns_labels_and_action():
    outcome = EsiCalculator().calculate(
        age=44,
        chief_complaint="mild headache",
        vitals=Vitals(pain_score=2),
    )
    assert outcome.esi_level in {1, 2, 3, 4, 5}
    assert outcome.priority
    assert outcome.recommended_action
    assert outcome.rationale
