from __future__ import annotations

from datetime import datetime

from fastapi import APIRouter
from pydantic import BaseModel

from app.workers.reminders import send_due_reminders

router = APIRouter(prefix="/api/v1/notifications", tags=["notifications"])


@router.get("/status")
async def status() -> dict[str, str]:
    return {"status": "ok"}


class ReminderAppointment(BaseModel):
    appointment_id: str
    first_name: str | None = None
    last_name: str | None = None
    email: str | None = None
    phone: str | None = None
    slot_start: datetime | None = None
    doctor_id: str | None = None


class ReminderRunRequest(BaseModel):
    appointments: list[ReminderAppointment] = []


class ReminderRunResponse(BaseModel):
    considered: int
    sent: int


@router.post("/internal/reminders/run", response_model=ReminderRunResponse)
async def run_reminders(request: ReminderRunRequest) -> ReminderRunResponse:
    """Admin-triggerable endpoint for the 24h-before appointment reminder job.

    This service has no scheduler of its own (see app/workers/reminders.py).
    An external trigger — a k8s CronJob or docker-compose cron sidecar — is
    expected to source the appointments due in the next reminder window
    (e.g. by querying appointment-service and enriching with patient contact
    details) and POST them here.
    """
    appointments = [a.model_dump(mode="json") for a in request.appointments]
    sent = await send_due_reminders(appointments)
    return ReminderRunResponse(considered=len(appointments), sent=sent)
