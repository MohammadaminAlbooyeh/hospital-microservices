from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.api.v1.routes import router as triage_router
from app.core.config import settings
from app.infrastructure.db import init_db
from app.infrastructure.kafka_consumer import build_consumer
from hospital_shared.errors import CorrelationIdMiddleware, install_exception_handlers
from hospital_shared.observability import setup_tracing

logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)

consumer = build_consumer()


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    if settings.kafka_consumer_enabled:
        try:
            await consumer.start()
        except Exception as exc:  # pragma: no cover - depends on Kafka availability
            logger.warning("Kafka consumer not started: %s", exc)
    yield
    if settings.kafka_consumer_enabled:
        await consumer.stop()


app = FastAPI(
    title="Hospital Triage Service",
    version="1.0.0",
    description="Emergency triage: ESI level 1-5 from vitals and chief complaint.",
    lifespan=lifespan,
)

app.add_middleware(CorrelationIdMiddleware)
install_exception_handlers(app)
setup_tracing(app, service_name=settings.service_name, endpoint=settings.otlp_endpoint)

Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)

app.include_router(triage_router)


@app.get("/health", tags=["ops"])
async def health() -> dict[str, str]:
    return {"status": "UP", "service": settings.service_name}
