"""Train the triage escalation model on synthetic data.

The label is "urgent or worse" (ESI <= 2) as determined by the deterministic
rule engine plus clinical noise. This keeps the model aligned with the rules
while adding a learned non-linear interaction between vitals.
"""

from __future__ import annotations

import random
from pathlib import Path

import joblib
import numpy as np
from sklearn.ensemble import RandomForestClassifier

from app.domain.esi_calculator import FEATURES
from app.domain.triage_rules import evaluate


class _Vitals:
    def __init__(self, **kwargs):
        for key, value in kwargs.items():
            setattr(self, key, value)


def _sample(rng: random.Random) -> tuple[list[float], int]:
    age = rng.randint(0, 95)
    temperature = round(rng.gauss(37.0, 1.1), 1)
    heart_rate = int(max(20, rng.gauss(82, 22)))
    systolic = int(max(50, rng.gauss(122, 24)))
    diastolic = int(max(30, rng.gauss(78, 15)))
    respiratory = int(max(4, rng.gauss(16, 6)))
    spo2 = int(min(100, max(60, rng.gauss(97, 5))))
    pain = rng.randint(0, 10)
    consciousness = rng.choices(
        ["alert", "voice", "pain", "unresponsive"], weights=[92, 4, 2, 2]
    )[0]

    vitals = _Vitals(
        temperature_c=temperature,
        heart_rate=heart_rate,
        systolic_bp=systolic,
        diastolic_bp=diastolic,
        respiratory_rate=respiratory,
        spo2=spo2,
        pain_score=pain,
        consciousness=consciousness,
    )
    result = evaluate(vitals, age=age, pregnancy=False, chief_complaint="")
    label = 1 if result.level <= 2 else 0
    if label == 0 and rng.random() < 0.05:
        label = 1
    features = [age, temperature, heart_rate, systolic, diastolic, respiratory, spo2, pain]
    return [float(f) for f in features], label


def train(output: Path = Path("app/ml/model.pkl"), samples: int = 6000) -> Path:
    rng = random.Random(42)
    rows = [_sample(rng) for _ in range(samples)]
    x = np.array([row[0] for row in rows])
    y = np.array([row[1] for row in rows])
    model = RandomForestClassifier(
        n_estimators=120,
        max_depth=7,
        min_samples_leaf=8,
        class_weight="balanced",
        random_state=42,
    )
    model.fit(x, y)
    model.feature_names = list(FEATURES) if hasattr(model, "__dict__") else None
    output.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(model, output)
    return output


if __name__ == "__main__":
    path = train()
    print(f"Triage model written to {path}")
