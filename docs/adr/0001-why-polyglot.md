# 0001 — Why polyglot (Java + Python)

Status: Accepted

## Context

The platform covers two very different kinds of work. The transactional core —
patients, appointment slots, encounters, invoices, payments — needs strong
consistency guarantees, mature ORM/migration tooling, and a stack with a long
track record in financial and healthcare systems. The intelligence and
integration layer — ESI triage scoring, no-show prediction, analytics
dashboards, PDF/OCR document handling, notification fan-out — benefits from a
fast-iterating ecosystem with first-class data science and parsing libraries.

No single stack is "better" across both. Forcing one language on both halves
means either a transactional core built on an immature ORM/locking story, or a
data-science layer fighting its tooling to call scikit-learn and pandas from a
JVM. The project also wants to demonstrate that a cross-language contract can
be managed deliberately, not accidentally.

## Decision

Split the 21 services across two stacks by capability, not by team preference:

| Capability | Stack | Why |
|---|---|---|
| Transactional core (patients, appointments, encounters, billing, payments) | Java 21 + Spring Boot 3 | Mature JPA/Hibernate optimistic locking, Flyway migrations, declarative `@Transactional`, strong typing for money and state machines, long history in regulated domains |
| Edge routing and identity | Java + Spring Cloud Gateway | Reactive filter chain, route predicates, rate limiting, mature Keycloak integration |
| ML, analytics, document parsing, messaging/integration | Python 3.12 + FastAPI | scikit-learn/pandas for ML and analytics, strong OCR/PDF libraries, fast notebook-to-service handoff, easy irregular-payload parsing |

The boundary rule is explicit: **the service that owns the aggregate writes
it; everyone else reacts to events or calls a documented query API.** A Python
service never writes into a Java service's tables, and a Java service never
reads a Python service's database directly. The only things shared across the
boundary are schema files (`contracts/avro`, `contracts/proto`) and Kafka
topics — never code, never a database connection.

## Consequences

- Two runtimes, two dependency trees, two CI pipelines (`java-ci.yml`,
  `python-ci.yml`), and two sets of operational conventions to maintain.
- The cost is contained by generating code from `contracts/` rather than
  hand-writing DTOs on both sides; a schema change is not "done" until both
  stacks compile against it and contract tests pass.
- Engineers need enough fluency in both stacks to read traces that cross the
  boundary (OpenTelemetry spans propagate React → gateway → Java → Kafka →
  Python), even if they are not expected to write both.
- Adding a 22nd service means picking a stack by the same capability rule
  above, not by whichever one the author prefers that week.
