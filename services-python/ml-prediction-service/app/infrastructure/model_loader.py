from __future__ import annotations

import logging
from pathlib import Path
from typing import Any

import joblib

logger = logging.getLogger(__name__)


def load_artifact(path: str | Path) -> dict[str, Any] | None:
    """Load a joblib-pickled model artifact produced by app.training.train_noshow.

    Returns None if the artifact is missing or cannot be loaded, so callers can
    fall back to a rule-based heuristic instead of crashing the service.
    """
    file_path = Path(path)
    if not file_path.exists():
        logger.warning("Model artifact not found at %s", file_path)
        return None
    try:
        return joblib.load(file_path)
    except Exception:  # pragma: no cover - defensive against corrupt artifacts
        logger.exception("Failed to load model artifact at %s", file_path)
        return None
