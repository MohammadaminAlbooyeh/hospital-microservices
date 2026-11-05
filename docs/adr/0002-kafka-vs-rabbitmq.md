# 0002 — Kafka over RabbitMQ as the event backbone

Status: Accepted

## Context

Every service in the platform publishes domain events through the
transactional outbox and reacts to events published by others — this is the
default communication mode ("asynchronous by default", `docs/architecture.md`
§2). The choice of broker therefore shapes how durable the event history is,
how ordering is guaranteed, and how the choreographed saga (invoice → payment
→ insurance → inventory) can be replayed and audited.

Two brokers were evaluated: Kafka and RabbitMQ.

RabbitMQ is a capable message queue with flexible routing (exchanges,
bindings) and is simpler to reason about for pure task-queue workloads such as
"send this one SMS". But the platform's needs go beyond task queues:

- `audit-service` must reconstruct a complete, ordered history of what
  happened to a patient record — an append-only requirement, not a
  consume-and-discard one.
- The saga (`docs/architecture.md` §6) needs to replay events after a bug fix
  or to rebuild a read model in `analytics-service` without re-querying every
  upstream service.
- Ordering matters per aggregate: all events for one `appointmentId` or
  `invoiceId` must be processed in the order they occurred, even if the
  aggregate is retried or compensated.
- The transactional outbox pattern (`docs/architecture.md` §5.3) is built
  around a relay that streams a database table to a log, which is Kafka's
  native use case; RabbitMQ has no equivalent durable log semantics once a
  message is acknowledged and removed from the queue.

## Decision

Use **Kafka** as the single event backbone for all 21 services.

- **Durability and replay.** Kafka retains events (7 days locally, see
  `docs/architecture.md` §7.3) rather than deleting them on consumption. A
  consumer with a bug can be fixed and replay the exact history; a new
  consumer (a future 22nd service) can read the backlog instead of only
  seeing events from the day it joined.
- **Ordering per partition.** The Kafka key is always the aggregate ID
  (`patientId`, `appointmentId`, `invoiceId`, `paymentId`, …). All events for
  one aggregate land in the same partition and are processed in commit order,
  which the saga's compensation logic depends on (a `PaymentFailed` must never
  be processed before the `InvoiceIssued` it responds to).
- **Ecosystem fit for outbox and event sourcing.** Kafka Connect/Debezium can
  read the outbox table via CDC with no application code once outbox volume
  justifies it (`docs/architecture.md` §5.3); the schema registry gives Avro
  schema evolution for `contracts/avro/*.avsc` with compatibility checks that
  RabbitMQ has no equivalent for.
- **Consumer groups over competing consumers.** Multiple independent
  consumers (`notification-service`, `ml-prediction-service`,
  `analytics-service`, `audit-service`) can each read the same topic at their
  own pace without the publisher fanning out N copies, which is awkward to
  express in RabbitMQ without multiple exchange bindings per consumer.

## Consequences

- Operational complexity is real: a schema registry, partition/replication
  planning (12 partitions, replication factor 3 in production —
  `docs/architecture.md` §5.2), consumer-group rebalancing, and a dead-letter
  topic (`hospital.events.dlq`) all need monitoring that a simple queue would
  not require.
- Kafka is not an XA participant, which is one of the reasons the saga is
  choreographed rather than using two-phase commit (see
  `0004-saga-over-two-phase-commit.md`).
- Delivery is at-least-once, never exactly-once end to end; every consumer
  must deduplicate via `processed_events` (`docs/architecture.md` §5.4). This
  is a deliberate trade for durability and replay, not an oversight.
- RabbitMQ (or SQS) remains a reasonable choice for a pure fire-and-forget
  task queue; `notification-service`'s internal Celery broker uses Redis for
  exactly that reason. Kafka is reserved for events that other services must
  be able to replay or reconstruct history from.
