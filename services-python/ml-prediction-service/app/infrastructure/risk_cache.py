from __future__ import annotations

"""In-memory cache of no-show risk scores keyed by appointment id.

Populated when the Kafka consumer handles an `AppointmentBooked` event, and
read back by the REST layer so a previously computed score can be served
without recomputing it. A real deployment would back this with Redis or the
service's own database; an in-memory dict is a pragmatic placeholder that
keeps this service self-contained for now.
"""

from dataclasses import dataclass
from threading import Lock


@dataclass(frozen=True)
class CachedRisk:
    appointment_id: str
    patient_id: str
    risk: float
    model_used: str


class RiskScoreCache:
    def __init__(self) -> None:
        self._lock = Lock()
        self._scores: dict[str, CachedRisk] = {}

    def set(self, entry: CachedRisk) -> None:
        with self._lock:
            self._scores[entry.appointment_id] = entry

    def get(self, appointment_id: str) -> CachedRisk | None:
        with self._lock:
            return self._scores.get(appointment_id)

    def __len__(self) -> int:
        with self._lock:
            return len(self._scores)


risk_cache = RiskScoreCache()
