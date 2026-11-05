"""Rule-based ESI screening.

The rules are deliberately conservative: a single critical finding is enough to
assign a high-acuity level. The ML model in :mod:`esi_calculator` only refines
borderline (ESI 3+) cases.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Protocol


class VitalsLike(Protocol):
    temperature_c: float | None
    heart_rate: int | None
    systolic_bp: int | None
    diastolic_bp: int | None
    respiratory_rate: int | None
    spo2: int | None
    pain_score: int
    consciousness: str


@dataclass(frozen=True)
class RuleResult:
    level: int
    reasons: list[str] = field(default_factory=list)


CRITICAL_COMPLAINTS = (
    "cardiac arrest",
    "not breathing",
    "unconscious",
    "seizure",
    "severe bleeding",
)

HIGH_COMPLAINTS = (
    "chest pain",
    "stroke",
    "shortness of breath",
    "suicidal",
    "anaphylaxis",
    "severe abdominal pain",
)


def evaluate(vitals: VitalsLike, age: int, pregnancy: bool = False, chief_complaint: str = "") -> RuleResult:
    level = 5
    reasons: list[str] = []
    complaint = chief_complaint.lower()

    temperature = vitals.temperature_c
    heart_rate = vitals.heart_rate
    systolic = vitals.systolic_bp
    diastolic = vitals.diastolic_bp
    respiratory = vitals.respiratory_rate
    spo2 = vitals.spo2
    pain = vitals.pain_score
    consciousness = (vitals.consciousness or "alert").lower()

    def escalate(candidate: int, reason: str) -> None:
        nonlocal level
        if candidate < level:
            level = candidate
        reasons.append(reason)

    # ---- ESI 1: immediate life-saving intervention -------------------------
    if consciousness == "unresponsive":
        escalate(1, "Patient is unresponsive")
    if spo2 is not None and spo2 < 85:
        escalate(1, f"Critical hypoxaemia (SpO2 {spo2}%)")
    if respiratory is not None and (respiratory < 8 or respiratory > 35):
        escalate(1, f"Critical respiratory rate ({respiratory}/min)")
    if systolic is not None and systolic < 70:
        escalate(1, f"Systolic blood pressure {systolic} mmHg")
    if heart_rate is not None and (heart_rate < 30 or heart_rate > 220):
        escalate(1, f"Critical heart rate ({heart_rate} bpm)")
    if temperature is not None and (temperature >= 41.0 or temperature <= 32.0):
        escalate(1, f"Critical temperature ({temperature} C)")
    if pain >= 10:
        escalate(1, "Maximum pain score")
    if any(flag in complaint for flag in CRITICAL_COMPLAINTS):
        escalate(1, f"Red-flag complaint: {chief_complaint}")

    # ---- ESI 2: high-risk / severe distress --------------------------------
    if consciousness in {"voice", "pain"}:
        escalate(2, f"Reduced consciousness ({consciousness})")
    if spo2 is not None and 85 <= spo2 < 92:
        escalate(2, f"Severe hypoxaemia (SpO2 {spo2}%)")
    if respiratory is not None and (8 <= respiratory < 10 or 30 < respiratory <= 35):
        escalate(2, f"Markedly abnormal respiratory rate ({respiratory}/min)")
    if systolic is not None and 70 <= systolic < 90:
        escalate(2, f"Hypotension (systolic {systolic} mmHg)")
    if heart_rate is not None and (30 <= heart_rate < 40 or 150 < heart_rate <= 220):
        escalate(2, f"Markedly abnormal heart rate ({heart_rate} bpm)")
    if temperature is not None and (39.5 <= temperature < 41.0 or 32.0 < temperature <= 34.0):
        escalate(2, f"Markedly abnormal temperature ({temperature} C)")
    if pain >= 7:
        escalate(2, f"Severe pain ({pain}/10)")
    if age >= 75 and temperature is not None and temperature >= 38.5:
        escalate(2, "Elderly patient with high fever")
    if age < 1 and temperature is not None and temperature >= 38.0:
        escalate(2, "Infant with fever")
    if pregnancy and "bleeding" in complaint:
        escalate(2, "Pregnant patient with bleeding")
    if any(flag in complaint for flag in HIGH_COMPLAINTS):
        escalate(2, f"High-risk complaint: {chief_complaint}")

    # ---- ESI 3: urgent, stable -------------------------------------------
    if spo2 is not None and 92 <= spo2 < 95:
        escalate(3, f"Mild hypoxaemia (SpO2 {spo2}%)")
    if respiratory is not None and 25 <= respiratory < 30:
        escalate(3, f"Tachypnoea ({respiratory}/min)")
    if systolic is not None and 90 <= systolic < 100:
        escalate(3, f"Borderline hypotension (systolic {systolic} mmHg)")
    if diastolic is not None and diastolic >= 110:
        escalate(3, f"Severe diastolic hypertension ({diastolic} mmHg)")
    if heart_rate is not None and 110 <= heart_rate <= 150:
        escalate(3, f"Tachycardia ({heart_rate} bpm)")
    if temperature is not None and 38.5 <= temperature < 39.5:
        escalate(3, f"Fever ({temperature} C)")
    if 4 <= pain <= 6:
        escalate(3, f"Moderate pain ({pain}/10)")
    if age >= 65 and any(
        [
            temperature is not None and temperature >= 38.0,
            heart_rate is not None and heart_rate > 100,
            systolic is not None and systolic < 100,
        ]
    ):
        escalate(3, "Elderly patient with abnormal vital signs")

    # ---- ESI 4: less urgent ----------------------------------------------
    if level == 5 and (1 <= pain <= 3 or temperature is not None and 37.8 <= temperature < 38.5):
        level = 4
        reasons.append("Minor symptoms suitable for semi-urgent care")

    return RuleResult(level=level, reasons=reasons)
