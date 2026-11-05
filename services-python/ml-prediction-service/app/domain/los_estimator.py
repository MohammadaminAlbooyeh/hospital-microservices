from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np

from app.infrastructure.model_loader import load_artifact
from app.training.feature_store import LOS_FEATURES, build_los_features


class LengthOfStayEstimator:
    """Wraps the trained length-of-stay RandomForestRegressor artifact.

    The model is trained on log1p(length_of_stay_days), so predictions are
    transformed back with expm1. Falls back to a simple heuristic when no
    trained artifact is available.
    """

    def __init__(self, model_path: str | Path, artifact: dict[str, Any] | None = None) -> None:
        self._model_path = Path(model_path)
        self._artifact = artifact
        self._loaded = artifact is not None

    def _load(self) -> dict[str, Any] | None:
        if not self._loaded:
            self._artifact = load_artifact(self._model_path)
            self._loaded = True
        return self._artifact

    @property
    def is_trained(self) -> bool:
        return self._load() is not None

    def predict(self, data: dict[str, Any]) -> tuple[float, str]:
        features = build_los_features(data)
        artifact = self._load()
        if artifact is not None:
            model = artifact["model"]
            ordered = [[features[name] for name in artifact.get("feature_names", LOS_FEATURES)]]
            predicted_days = float(np.expm1(model.predict(ordered)[0]))
            return round(max(0.1, predicted_days), 2), f"los-rf-{artifact.get('version', '1.0.0')}"
        return round(self._heuristic(features), 2), "los-heuristic"

    @staticmethod
    def _heuristic(features: dict[str, float]) -> float:
        days = (
            0.5
            + 0.6 * features["severity"]
            + 0.4 * features["diagnoses_count"]
            + 2.0 * features["is_inpatient"]
        )
        return max(0.2, min(45.0, days))
