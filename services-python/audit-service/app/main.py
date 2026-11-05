from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.api.v1.routes import router
from app.core.config import settings
from app.infrastructure.db import init_db
from hospital_shared.errors import CorrelationIdMiddleware, install_exception_handlers
from hospital_shared.observability import setup_tracing

logging.basicConfig(level=getattr(logging, settings.log_level.upper(), logging.INFO), format="%(asctime)s %(levelname)s %(name)s - %(message)s")


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    yield


app = FastAPI(title="Hospital Audit Service", version="1.0.0", description="Immutable audit trail for patient data access and system events.", lifespan=lifespan)
app.add_middleware(CorrelationIdMiddleware)
install_exception_handlers(app)
setup_tracing(app, service_name=settings.service_name, endpoint=settings.otlp_endpoint)
Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)
app.include_router(router)


@app.get("/health", tags=["ops"])
async def health() -> dict[str, str]:
    return {"status": "UP", "service": settings.service_name}
