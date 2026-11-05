from __future__ import annotations

from pathlib import Path
from typing import Any

from app.infrastructure.model_loader import load_artifact
from app.training.feature_store import NO_SHOW_FEATURES, build_no_show_features


class NoShowPredictor:
    """Wraps the trained no-show RandomForestClassifier artifact.

    Falls back to a simple rule-based score when the artifact has not been
    trained yet, so the API stays usable before/without `train_noshow.py`
    having been run (e.g. in a minimal dev environment).
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
        features = build_no_show_features(data)
        artifact = self._load()
        if artifact is not None:
            model = artifact["model"]
            ordered = [[features[name] for name in artifact.get("feature_names", NO_SHOW_FEATURES)]]
            probability = float(model.predict_proba(ordered)[0][1])
            return round(probability, 4), f"noshow-rf-{artifact.get('version', '1.0.0')}"
        return round(self._heuristic(features), 4), "noshow-heuristic"

    @staticmethod
    def _heuristic(features: dict[str, float]) -> float:
        risk = (
            0.05
            + 0.004 * features["lead_days"]
            + 0.08 * features["prior_no_shows"]
            + 0.002 * features["distance_km"]
            + 0.1 * features["is_new_patient"]
            - 0.05 * features["has_insurance"]
        )
        return min(0.99, max(0.01, risk))
