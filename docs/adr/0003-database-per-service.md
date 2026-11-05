# 0003 — Database per service

Status: Accepted

## Context

`docs/architecture.md` §2 states the principle plainly: "Each service owns its
schema and is the only writer. There are no cross-service joins and no shared
tables." The alternative — one shared schema (or one shared database instance
with cross-service foreign keys) — is common in systems that grew from a
monolith, and it is tempting here too: a shared `patients` table would make it
trivial for `billing-service` to join against `encounters` and `patients` in
one SQL query instead of assembling data from events and query APIs.

That convenience is exactly the coupling the platform is trying to avoid.
With 21 independently deployable services, a shared schema means:

- A migration in one service can break a query written by another service
  against the same tables, with no compiler or test to catch it before
  deploy.
- Two services competing for locks on the same rows during their own
  transactions (classic hot-row contention), even when their business logic
  has nothing to do with each other.
- No way to pick the right storage engine per workload. `ehr-service` needs
  an append-only, flexible document shape (clinical notes vary in structure);
  `analytics-service` needs time-series aggregation; `api-gateway` needs
  nothing but a fast counter store for rate limiting. One shared PostgreSQL
  schema forces every one of those into the same relational model.
- A service cannot be deployed, scaled, or restored independently if its data
  lives in the same database as five other services.

## Decision

Every service owns exactly one database (or document store) and is the only
writer to it. Downstream services never read another service's tables
directly:

- PostgreSQL for the transactional services (`hospital_patient`,
  `hospital_appointment`, `hospital_billing`, `hospital_payment`, …).
- MongoDB for `ehr-service` (`hospital_ehr`) — append-only, versioned
  clinical documents don't fit a fixed relational shape well.
- TimescaleDB for `analytics-service` (`hospital_analytics`) — hypertables
  for wait-time, occupancy, and revenue time series.
- Redis for `api-gateway` rate-limit counters and the `notification-service`
  Celery broker — no business data, just ephemeral state.

This is intentional polyglot persistence, not just polyglot compute (see
`0001-why-polyglot.md`). The only coupling between services is an opaque ID
(`patientId`, `encounterId`, `invoiceId`) and the event/query contract. A
screen that needs data from several services is assembled by the gateway/BFF,
or by a service that subscribes to events and keeps its own local read model —
`analytics-service`'s `AppointmentFact`/`EncounterFact`/`InvoiceFact` tables
are exactly this pattern.

Referential integrity is therefore local only: a foreign key never points
across a database boundary. If `patient-service` deletes or merges a record,
downstream services learn about it through an event, not a cascading delete.
Migrations are owned per service — Flyway for Java, Alembic for Python — and
a migration only ever touches its own database.

## Consequences

- No cheap cross-service SQL joins. Reporting and dashboard queries
  (`analytics-service`) must be built from events into local read models
  ahead of time, not issued ad hoc against someone else's tables.
- Each service can choose the storage engine that fits its data shape and
  can be scaled, backed up, and restored independently — because there are no
  cross-service foreign keys, a restore can be validated service by service
  (`docs/architecture.md` §7.3).
- Eventual consistency is a fact of life: a read model built from events can
  lag the source of truth by the time it takes the event to be published and
  consumed. The saga and idempotency rules (`docs/architecture.md` §5.4, §6)
  exist precisely because the platform does not pretend this lag away.
- Operationally this means 21 logical databases to provision and migrate
  instead of one. Local development may run them all inside one PostgreSQL
  instance as separate logical databases without weakening the boundary —
  the separation is about ownership, not physical server count.
