from __future__ import annotations

import math
from dataclasses import dataclass, field
from pathlib import Path

from app.domain.triage_rules import VitalsLike, evaluate

PRIORITY_LABELS = {
    1: "IMMEDIATE",
    2: "EMERGENT",
    3: "URGENT",
    4: "SEMI_URGENT",
    5: "NON_URGENT",
}

RECOMMENDED_ACTIONS = {
    1: "Resuscitation room immediately; senior clinician at bedside",
    2: "Assess within 10 minutes; continuous monitoring",
    3: "Assess within 30 minutes; recheck vitals every 15 minutes",
    4: "Assess within 60 minutes; standard observation",
    5: "Fast-track / outpatient pathway",
}

FEATURES = (
    "age",
    "temperature_c",
    "heart_rate",
    "systolic_bp",
    "diastolic_bp",
    "respiratory_rate",
    "spo2",
    "pain_score",
)


@dataclass(frozen=True)
class TriageOutcome:
    esi_level: int
    priority: str
    recommended_action: str
    rationale: list[str] = field(default_factory=list)
    model_used: str = "rules"


class EsiCalculator:
    """Combines deterministic clinical rules with an optional ML refinement."""

    def __init__(self, model_path: str | Path | None = None) -> None:
        self._model_path = Path(model_path) if model_path else None
        self._model = None
        self._model_loaded = False

    def _load_model(self):
        if self._model_loaded:
            return self._model
        self._model_loaded = True
        if self._model_path is None or not self._model_path.exists():
            return None
        try:
            import joblib

            self._model = joblib.load(self._model_path)
        except Exception:
            self._model = None
        return self._model

    def _probability(self, age: int, vitals: VitalsLike) -> float | None:
        model = self._load_model()
        if model is None:
            return None
        features = [[
            float(age),
            float(vitals.temperature_c if vitals.temperature_c is not None else 37.0),
            float(vitals.heart_rate if vitals.heart_rate is not None else 80),
            float(vitals.systolic_bp if vitals.systolic_bp is not None else 120),
            float(vitals.diastolic_bp if vitals.diastolic_bp is not None else 80),
            float(vitals.respiratory_rate if vitals.respiratory_rate is not None else 16),
            float(vitals.spo2 if vitals.spo2 is not None else 98),
            float(vitals.pain_score),
        ]]
        try:
            if hasattr(model, "predict_proba"):
                return float(model.predict_proba(features)[0][1])
            if isinstance(model, dict):
                weights = model.get("weights", [])
                bias = float(model.get("bias", 0.0))
                score = bias + sum(w * x for w, x in zip(weights, features[0], strict=False))
                return 1.0 / (1.0 + math.exp(-score))
        except Exception:
            return None
        return None

    def calculate(
        self,
        *,
        age: int,
        chief_complaint: str,
        vitals: VitalsLike,
        pregnancy: bool = False,
    ) -> TriageOutcome:
        result = evaluate(vitals, age=age, pregnancy=pregnancy, chief_complaint=chief_complaint)
        level = result.level
        rationale = list(result.reasons)
        model_used = "rules"

        if level >= 3:
            probability = self._probability(age, vitals)
            if probability is not None:
                model_used = "rules+ml"
                if probability >= 0.65 and level > 1:
                    level -= 1
                    rationale.append(
                        f"ML model escalated acuity (urgent-risk probability {probability:.2f})"
                    )
                elif probability <= 0.15:
                    rationale.append(
                        f"ML model found low urgent-risk probability ({probability:.2f})"
                    )

        if not rationale:
            rationale.append("No abnormal findings; routine presentation")

        return TriageOutcome(
            esi_level=level,
            priority=PRIORITY_LABELS[level],
            recommended_action=RECOMMENDED_ACTIONS[level],
            rationale=rationale,
            model_used=model_used,
        )
