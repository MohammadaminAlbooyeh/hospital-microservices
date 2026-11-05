from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.metrics import accuracy_score, mean_absolute_error, r2_score, roc_auc_score
from sklearn.model_selection import train_test_split

from app.training.feature_store import (
    LOS_FEATURES,
    NO_SHOW_FEATURES,
    generate_los_dataset,
    generate_no_show_dataset,
)

NO_SHOW_MODEL_FILE = "no_show_model.joblib"
LOS_MODEL_FILE = "length_of_stay_model.joblib"
METADATA_FILE = "metadata.json"
MODEL_VERSION = "1.0.0"


def default_model_dir() -> Path:
    return Path(__file__).resolve().parents[1] / "models"


def _reference_means(dataset, features: list[str]) -> dict[str, float]:
    return {feature: float(dataset[feature].mean()) for feature in features}


def train_no_show_model(dataset, random_state: int = 42) -> dict:
    features = dataset[NO_SHOW_FEATURES].astype(float)
    target = dataset["no_show"].astype(int)
    x_train, x_test, y_train, y_test = train_test_split(
        features, target, test_size=0.2, random_state=random_state, stratify=target
    )
    model = RandomForestClassifier(
        n_estimators=180,
        max_depth=9,
        min_samples_leaf=4,
        class_weight="balanced_subsample",
        random_state=random_state,
        n_jobs=-1,
    )
    model.fit(x_train, y_train)
    probabilities = model.predict_proba(x_test)[:, 1]
    predictions = (probabilities >= 0.5).astype(int)
    metrics = {
        "accuracy": round(float(accuracy_score(y_test, predictions)), 4),
        "roc_auc": round(float(roc_auc_score(y_test, probabilities)), 4),
        "positive_rate": round(float(target.mean()), 4),
        "samples": int(len(dataset)),
    }
    return {
        "model": model,
        "feature_names": list(NO_SHOW_FEATURES),
        "reference_means": _reference_means(dataset, NO_SHOW_FEATURES),
        "version": MODEL_VERSION,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "metrics": metrics,
    }


def train_los_model(dataset, random_state: int = 42) -> dict:
    features = dataset[LOS_FEATURES].astype(float)
    target = dataset["length_of_stay_days"].astype(float)
    x_train, x_test, y_train, y_test = train_test_split(
        features, target, test_size=0.2, random_state=random_state
    )
    model = RandomForestRegressor(
        n_estimators=180,
        max_depth=11,
        min_samples_leaf=3,
        random_state=random_state,
        n_jobs=-1,
    )
    model.fit(x_train, np.log1p(y_train))
    predicted_days = np.expm1(model.predict(x_test))
    metrics = {
        "mae_days": round(float(mean_absolute_error(y_test, predicted_days)), 4),
        "r2": round(float(r2_score(y_test, predicted_days)), 4),
        "samples": int(len(dataset)),
    }
    return {
        "model": model,
        "feature_names": list(LOS_FEATURES),
        "reference_means": _reference_means(dataset, LOS_FEATURES),
        "version": MODEL_VERSION,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "metrics": metrics,
    }


def _metadata(no_show: dict, los: dict) -> dict:
    return {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "no_show": {
            "version": no_show["version"],
            "trained_at": no_show["trained_at"],
            "features": no_show["feature_names"],
            "metrics": no_show["metrics"],
        },
        "length_of_stay": {
            "version": los["version"],
            "trained_at": los["trained_at"],
            "features": los["feature_names"],
            "metrics": los["metrics"],
        },
    }


def train_all(
    model_dir: str | Path | None = None,
    n_samples: int = 2400,
    random_state: int = 42,
) -> dict:
    output = Path(model_dir) if model_dir else default_model_dir()
    output.mkdir(parents=True, exist_ok=True)

    no_show_dataset = generate_no_show_dataset(n_samples=n_samples, random_state=random_state)
    los_dataset = generate_los_dataset(n_samples=n_samples, random_state=random_state)

    no_show_artifact = train_no_show_model(no_show_dataset, random_state=random_state)
    los_artifact = train_los_model(los_dataset, random_state=random_state)

    joblib.dump(no_show_artifact, output / NO_SHOW_MODEL_FILE)
    joblib.dump(los_artifact, output / LOS_MODEL_FILE)
    metadata = _metadata(no_show_artifact, los_artifact)
    (output / METADATA_FILE).write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    return metadata


def main() -> None:
    parser = argparse.ArgumentParser(description="Train hospital ML prediction models on synthetic data")
    parser.add_argument("--output-dir", default=str(default_model_dir()))
    parser.add_argument("--samples", type=int, default=2400)
    parser.add_argument("--seed", type=int, default=42)
    arguments = parser.parse_args()
    metadata = train_all(arguments.output_dir, n_samples=arguments.samples, random_state=arguments.seed)
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
