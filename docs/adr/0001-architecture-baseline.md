# ADR 0001: Architecture Baseline

- Status: Accepted
- Date: 2026-11-05

The hospital platform uses a polyglot microservice architecture (Java/Spring Boot for transactional core, Python/FastAPI for async/intelligence services), PostgreSQL per service, Kafka for domain events, and an API gateway with centralized auth.
