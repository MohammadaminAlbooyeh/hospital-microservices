from __future__ import annotations

from datetime import date, datetime, time, timezone


def resolve_window(from_date: date | None, to_date: date | None) -> tuple[datetime | None, datetime | None]:
    if from_date is not None and to_date is not None and from_date > to_date:
        raise ValueError("'from' must be earlier than or equal to 'to'")
    start = datetime.combine(from_date, time.min, tzinfo=timezone.utc) if from_date else None
    end = datetime.combine(to_date, time.max, tzinfo=timezone.utc) if to_date else None
    return start, end
