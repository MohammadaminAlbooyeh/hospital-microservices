from __future__ import annotations

import logging

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.api.v1.routes import router
from app.core.config import settings
from hospital_shared.errors import CorrelationIdMiddleware, install_exception_handlers
from hospital_shared.observability import setup_tracing

logging.basicConfig(level=getattr(logging, settings.log_level.upper(), logging.INFO), format="%(asctime)s %(levelname)s %(name)s - %(message)s")

app = FastAPI(title="Hospital ML Prediction Service", version="1.0.0", description="ML prediction APIs (training assets included; runtime model loading can replace placeholder).")
app.add_middleware(CorrelationIdMiddleware)
install_exception_handlers(app)
setup_tracing(app, service_name=settings.service_name, endpoint=settings.otlp_endpoint)
Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)
app.include_router(router)


@app.get("/health", tags=["ops"])
async def health() -> dict[str, str]:
    return {"status": "UP", "service": settings.service_name}
