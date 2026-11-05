from __future__ import annotations

from fastapi import APIRouter

router = APIRouter(prefix="/api/v1/notifications", tags=["notifications"])


@router.get("/status")
async def status() -> dict[str, str]:
    return {"status": "ok"}
