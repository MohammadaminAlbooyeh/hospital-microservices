from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.domain.no_show_predictor import NoShowPredictor

# Clinic opens 7:00, last bookable slot 18:00, as used by the no-show
# training data generator (feature_store.generate_no_show_dataset).
CANDIDATE_HOURS = list(range(7, 19))
CANDIDATE_DAYS = list(range(0, 7))  # 0=Monday ... 6=Sunday


@dataclass(frozen=True)
class SlotSuggestion:
    day_of_week: int
    appointment_hour: int
    predicted_risk: float


class OptimalSlotSuggester:
    """Pragmatic heuristic: scan a small grid of day/hour candidates through
    the no-show predictor and return the lowest-risk slot(s).

    This is intentionally not a research-grade optimizer (no joint
    scheduling/capacity constraints) -- it reuses the already-trained
    no-show model to rank candidate slots for a single patient.
    """

    def __init__(self, no_show_predictor: NoShowPredictor) -> None:
        self._no_show_predictor = no_show_predictor

    def suggest(
        self,
        data: dict[str, Any],
        top_n: int = 3,
        candidate_days: list[int] | None = None,
        candidate_hours: list[int] | None = None,
    ) -> list[SlotSuggestion]:
        days = candidate_days or CANDIDATE_DAYS
        hours = candidate_hours or CANDIDATE_HOURS
        scored: list[SlotSuggestion] = []
        for day in days:
            for hour in hours:
                candidate = {**data, "day_of_week": day, "appointment_hour": hour}
                risk, _ = self._no_show_predictor.predict(candidate)
                scored.append(SlotSuggestion(day_of_week=day, appointment_hour=hour, predicted_risk=risk))
        scored.sort(key=lambda slot: slot.predicted_risk)
        return scored[: max(1, top_n)]
