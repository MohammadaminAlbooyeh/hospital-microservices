from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.api.v1.routes import router
from app.core.config import settings
from app.infrastructure.kafka_consumer import build_consumer
from hospital_shared.errors import CorrelationIdMiddleware, install_exception_handlers
from hospital_shared.observability import setup_tracing

logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)


def _ensure_models_trained() -> None:
    model_dir = Path(settings.model_dir)
    no_show_path = model_dir / settings.no_show_model_file
    los_path = model_dir / settings.length_of_stay_model_file
    if no_show_path.exists() and los_path.exists():
        return
    if not settings.auto_train_on_startup:
        logger.warning(
            "Model artifacts missing at %s and auto_train_on_startup is disabled; "
            "predictions will fall back to heuristics",
            model_dir,
        )
        return
    logger.info("Model artifacts missing; training on synthetic data (model_dir=%s)", model_dir)
    from app.training.train_noshow import train_all

    train_all(model_dir=model_dir)


@asynccontextmanager
async def lifespan(app: FastAPI):
    _ensure_models_trained()
    consumer = None
    if settings.kafka_consumer_enabled:
        try:
            consumer = build_consumer()
            await consumer.start()
        except Exception as exc:  # pragma: no cover - depends on Kafka availability
            logger.warning("Kafka consumer not started: %s", exc)
            consumer = None
    yield
    if consumer is not None:
        await consumer.stop()


app = FastAPI(
    title="Hospital ML Prediction Service",
    version="1.0.0",
    description="No-show risk, length-of-stay estimation and optimal slot suggestion.",
    lifespan=lifespan,
)
app.add_middleware(CorrelationIdMiddleware)
install_exception_handlers(app)
setup_tracing(app, service_name=settings.service_name, endpoint=settings.otlp_endpoint)
Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)
app.include_router(router)


@app.get("/health", tags=["ops"])
async def health() -> dict[str, str]:
    return {"status": "UP", "service": settings.service_name}
