from __future__ import annotations

import logging
from typing import Any

logger = logging.getLogger(__name__)


def setup_tracing(app: Any = None, service_name: str = "hospital-service", endpoint: str | None = None) -> None:
    """Configure OpenTelemetry tracing. Failures never break the service."""
    try:
        from opentelemetry import trace
        from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
        from opentelemetry.sdk.resources import Resource
        from opentelemetry.sdk.trace import TracerProvider
        from opentelemetry.sdk.trace.export import BatchSpanProcessor

        resource = Resource.create({"service.name": service_name})
        provider = TracerProvider(resource=resource)
        if endpoint:
            provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter(endpoint=endpoint, insecure=True)))
        trace.set_tracer_provider(provider)

        if app is not None:
            from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor

            FastAPIInstrumentor.instrument_app(app)
        logger.info("OpenTelemetry tracing enabled for %s", service_name)
    except Exception as exc:  # pragma: no cover - optional dependency at runtime
        logger.warning("Tracing not configured: %s", exc)
