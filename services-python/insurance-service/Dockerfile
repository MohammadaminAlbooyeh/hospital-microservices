# Build context: repository root
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential libpq-dev curl \
    && rm -rf /var/lib/apt/lists/*

COPY services-python/shared /app/shared
COPY services-python/insurance-service /app/service
RUN pip install /app/shared && pip install /app/service

WORKDIR /app/service

RUN addgroup --system hospital && adduser --system --ingroup hospital hospital \
    && chown -R hospital:hospital /app
USER hospital

EXPOSE 8002
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -fsS http://localhost:8002/health || exit 1
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8002"]
