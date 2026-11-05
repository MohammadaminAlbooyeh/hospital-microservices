# Hospital Platform Architecture

Status: Phase one (outpatient journey) — accepted baseline  
Last updated: 2026-11-05

## 1. Context

The platform is a hospital information system covering the outpatient journey:
registration, appointment booking, check-in, triage, consultation, laboratory,
pharmacy, discharge, invoicing, insurance adjudication, and payment. It is
implemented as 21 independently deployable services across Java/Spring Boot and
Python/FastAPI, connected by REST/gRPC for synchronous questions and Kafka for
domain events.

The system is deliberately shaped like a production system rather than a CRUD
demo. The difficult parts are explicit:

- concurrent appointment booking must never double-book a slot;
- an encounter is a state machine, not a set of booleans;
- a database write and its Kafka event must not diverge (transactional outbox);
- invoice, payment, insurance, and inventory form a distributed workflow that
  needs compensation rather than a distributed transaction;
- every read of patient data must be auditable, and every service must honour
  role-based access control.

### Actors

| Actor | Primary entry point | Typical actions |
|---|---|---|
| Patient | `patient-portal` | Register, book/cancel appointments, view results and invoices, pay |
| Doctor | `staff-console` | Open encounters, write notes, order labs, prescribe, discharge |
| Nurse | `staff-console` | Check in, triage, vitals, medication administration support |
| Receptionist | `staff-console` | Register patients, book/reschedule, manage demographics |
| Billing staff | `staff-console` | Review invoices, insurance claims, payments, refunds |
| Administrator | `admin-dashboard` | Manage users/roles, view analytics, inspect audit trails |
| External payer | Insurance clearinghouse | Eligibility responses and claim adjudication |
| External provider | Payment gateway / SMS / email | Card capture, notifications |

### System context

```
Patients / clinical staff / administrators
                │  HTTPS
                ▼
        API Gateway (JWT, rate limit, CORS, correlation)
                │
   ┌────────────┴───────────────────────────────┐
   │           21 Spring Boot / FastAPI services │
   │   REST + gRPC for queries, Kafka for events │
   └────────────┬───────────────────────────────┘
                │
   PostgreSQL · MongoDB · TimescaleDB · Redis · Kafka
                │
   Keycloak · payment gateway · SMS/email · insurance clearinghouse
```

The C4 context and container models are in `docs/diagrams/c4-context.puml` and
`docs/diagrams/c4-container.puml`. The end-to-end journey is in
`docs/diagrams/patient-journey-sequence.puml`.

## 2. Architectural principles

1. **Database per service.** Each service owns its schema and is the only
   writer. There are no cross-service joins and no shared tables.
2. **Asynchronous by default.** A service publishes a domain event when
   something happened and does not wait for unrelated services. Synchronous
   REST/gRPC is reserved for queries whose caller needs the answer in the same
   request.
3. **Contracts are code.** Avro schemas are the Kafka contract; protobuf is the
   synchronous contract. Both stacks generate models from the same files under
   `contracts/`.
4. **No dual writes.** The business row and the outbox row commit in the same
   local transaction. A relay publishes the outbox afterwards.
5. **At-least-once everywhere.** Producers retry; consumers deduplicate. Every
   externally visible command carries an idempotency key.
6. **Explicit workflows.** Appointment booking uses optimistic locking, the
   encounter uses a state machine, and the money/inventory flow uses a
   choreographed saga with named compensation actions.
7. **Least privilege and auditability.** RBAC is enforced at the gateway and
   again in each service; access to patient data is recorded by audit-service.

## 3. Service catalog

The platform has 21 services: 14 Java/Spring Boot services (including the
gateway, auth, and config/discovery), 7 Python/FastAPI services. Ports are the
host-side defaults from `.env.example`; container-to-container traffic uses the
same port on the Docker network.

### A. Infrastructure and security

| # | Service | Stack | Port | Database / store | Responsibility |
|---|---|---|---|---|---|
| 1 | `api-gateway` | Java 21, Spring Cloud Gateway | 8080 | Redis (rate-limit counters) | Single entry point: routing, JWT validation, rate limiting, CORS, correlation ID, response compression |
| 2 | `auth-service` | Java 21 + Keycloak | 8081 | PostgreSQL `hospital_auth`; Keycloak realm `hospital` | Registration, login, OTP, refresh tokens, role assignment; fronts Keycloak |
| 3 | `config-discovery-service` | Java 21, Spring Cloud Config + Eureka | 8888 | PostgreSQL `hospital_config` | Centralised configuration and service registry |

### B. Patient core domain

| # | Service | Stack | Port | Database / store | Responsibility |
|---|---|---|---|---|---|
| 4 | `patient-service` | Java 21 | 8082 | PostgreSQL `hospital_patient` | System of record for demographics, contact, insurance policy, allergies, emergency contacts |
| 5 | `appointment-service` | Java 21 | 8083 | PostgreSQL `hospital_appointment` | Slot inventory, booking, rescheduling, cancellation; optimistic locking |
| 6 | `doctor-service` | Java 21 | 8084 | PostgreSQL `hospital_doctor` | Doctor profiles, specialities, weekly schedules, leave, capacity |
| 7 | `department-service` | Java 21 | 8085 | PostgreSQL `hospital_department` | Departments, rooms, beds, equipment |
| 8 | `triage-service` | Python 3.12, FastAPI | 8001 | PostgreSQL `hospital_triage` | ESI level 1–5 from vitals and chief complaint (rules + ML model) |

### C. Clinical workflow

| # | Service | Stack | Port | Database / store | Responsibility |
|---|---|---|---|---|---|
| 9 | `encounter-service` | Java 21 | 8086 | PostgreSQL `hospital_encounter` | Visit entity and explicit state machine; orchestrates the clinical lifecycle |
| 10 | `ehr-service` | Java 21 | 8087 | MongoDB `hospital_ehr` | Append-only, versioned clinical notes, ICD-10 diagnoses, history |
| 11 | `prescription-service` | Java 21 | 8088 | PostgreSQL `hospital_prescription` | Prescribing, dosage, interaction checks (delegated to a Python rule service) |
| 12 | `lab-service` | Java 21 | 8089 | PostgreSQL `hospital_lab` | Lab orders, specimen tracking, results, imaging referral to PACS |
| 13 | `pharmacy-inventory-service` | Java 21 | 8090 | PostgreSQL `hospital_pharmacy` | Stock levels, dispensing, reservations, low-stock and expiry alerts |

### D. Financial

| # | Service | Stack | Port | Database / store | Responsibility |
|---|---|---|---|---|---|
| 14 | `billing-service` | Java 21 | 8091 | PostgreSQL `hospital_billing` | Invoice assembly from encounter charges; insurer/patient split; voiding |
| 15 | `payment-service` | Java 21 | 8092 | PostgreSQL `hospital_payment` | Gateway integration, instalments, refunds, idempotent payment attempts |
| 16 | `insurance-service` | Python 3.12, FastAPI | 8002 | PostgreSQL `hospital_insurance` | Eligibility checks, claim submission, claim status polling, rejection handling |

### E. Intelligence and supporting services

| # | Service | Stack | Port | Database / store | Responsibility |
|---|---|---|---|---|---|
| 17 | `notification-service` | Python 3.12, FastAPI + Celery | 8003 | PostgreSQL `hospital_notification`; Redis broker | Email/SMS/push, reminders, lab and payment alerts; Kafka consumer |
| 18 | `analytics-service` | Python 3.12, pandas | 8004 | TimescaleDB `hospital_analytics` | Wait time, no-show rate, bed occupancy, revenue by department |
| 19 | `ml-prediction-service` | Python 3.12, scikit-learn | 8005 | PostgreSQL `hospital_ml` | No-show prediction, length-of-stay estimate, slot suggestions |
| 20 | `document-service` | Python 3.12, FastAPI | 8006 | PostgreSQL `hospital_document`; object storage | PDF invoices, receipts, discharge summaries, certificates; OCR for uploads |
| 21 | `audit-service` | Python 3.12, FastAPI | 8007 | PostgreSQL `hospital_audit` | Immutable access log: who read or changed which patient data, when |

Database names are logical: local development may use one PostgreSQL instance
with 21 databases; production uses managed instances or clusters per service
group. Keycloak owns its own `keycloak` database and is not part of the
service-owned schema list.

## 4. Why polyglot

The split follows capability, not fashion.

| Capability | Chosen stack | Why |
|---|---|---|
| Transactional core: patients, slots, encounters, invoices, payments | Java 21 + Spring Boot 3 | Mature JPA/Hibernate locking, Flyway migrations, declarative transactions, strong typing, long-lived financial/healthcare ecosystem |
| Edge routing and identity | Java + Spring Cloud Gateway | Reactive filter chain, route predicates, rate limiting, Keycloak integration |
| ML, analytics, document parsing | Python 3.12 + FastAPI | scikit-learn/pandas, rapid iteration, strong OCR/PDF libraries, easy notebook-to-service handoff |
| Messaging and integration | Both stacks | Kafka clients exist for both; Avro schemas are the shared language |

The cost is two runtimes, two dependency trees, and two CI pipelines. That cost
is contained by making the contracts generated artifacts (`contracts/proto` and
`contracts/avro`) rather than hand-written DTOs. The Java side never imports
Python code and vice versa; the only shared thing is a schema file.

The boundary rule: **the service that owns the aggregate writes it; everyone
else reacts to events or calls a documented query API.** A Python service must
not write into a Java service's tables, and a Java service must not scrape a
Python service's database.

## 5. Event-driven architecture

### 5.1 When to use which transport

| Need | Transport | Example |
|---|---|---|
| Caller needs the answer now | REST through the gateway or gRPC | Get patient by ID; check insurance eligibility; get invoice |
| Side effect can happen later | Kafka event | Send welcome SMS; update analytics; generate PDF receipt |
| Durable workflow across services | Kafka events + compensations | Invoice/payment/inventory saga |
| High-volume internal query | gRPC | Python triage resolving a patient before scoring |

Synchronous calls are wrapped with timeouts and circuit breakers. A failed
synchronous call never becomes an unrecorded event: the caller either fails the
request or writes its own state and publishes an event through the outbox.

### 5.2 Topic and event catalog

Phase one uses one domain topic. The Kafka key is the aggregate ID, so all
events for one patient/appointment/invoice stay ordered within a partition.
Production creates 12 partitions with replication factor 3; local Compose uses
1 partition. The operational dead-letter topic is `hospital.events.dlq`.

| Event | Avro schema | Producer | Consumers | Trigger | Kafka key |
|---|---|---|---|---|---|
| `PatientRegistered` | `PatientRegistered.avsc` | patient-service | notification-service (welcome), analytics-service (funnel), audit-service, insurance-service (pre-check) | Patient row committed | `patientId` |
| `AppointmentBooked` | `AppointmentBooked.avsc` | appointment-service | notification-service (SMS), ml-prediction-service (features/risk), analytics-service, audit-service | Slot locked and appointment committed | `appointmentId` |
| `AppointmentCancelled` | `AppointmentCancelled.avsc` | appointment-service | notification-service, ml-prediction-service, analytics-service, audit-service | Cancellation/reschedule committed | `appointmentId` |
| `EncounterCompleted` | `EncounterCompleted.avsc` | encounter-service | billing-service (invoice), insurance-service, analytics-service, audit-service | State machine reaches `COMPLETED` | `encounterId` |
| `LabResultReady` | `LabResultReady.avsc` | lab-service | notification-service, encounter-service, ehr-service, billing-service, analytics-service, audit-service | Result verified and released | `labOrderId` |
| `InvoiceIssued` | `InvoiceIssued.avsc` | billing-service | notification-service, insurance-service, document-service, analytics-service, audit-service | Invoice issued with insurer/patient split | `invoiceId` |
| `PaymentCompleted` | `PaymentCompleted.avsc` | payment-service | billing-service (close invoice), document-service (receipt), notification-service, analytics-service, audit-service | Gateway confirms capture | `paymentId` |
| `PaymentFailed` | `PaymentFailed.avsc` | payment-service | billing-service (revert invoice), pharmacy-inventory-service (stock), notification-service, analytics-service, audit-service | Gateway declines or errors | `paymentId` |

Envelope convention:

- **Key** — aggregate ID as above.
- **Headers** — `eventId`, `eventType`, `occurredAt`, `aggregateId`,
  `correlationId`, `contentType`.
- **Value** — the Avro record for the event, serialised with the schema
  registered in the schema registry.
- **Delivery** — at-least-once; consumers are idempotent; offsets commit after
  the business transaction.

The machine-readable contract is `docs/events/asyncapi.yaml`; the schemas live
in `contracts/avro/`. Java models mirror
`com.hospital.shared.events.EventEnvelope`; Python models mirror
`hospital_shared.events.models.EventEnvelope` and the eight typed payloads.

### 5.3 Transactional outbox

Each service owns an `outbox` table in its own database:

```sql
CREATE TABLE outbox (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(64)  NOT NULL,
    aggregate_id    VARCHAR(128) NOT NULL,
    event_type      VARCHAR(128) NOT NULL,
    payload         JSONB        NOT NULL,   -- Avro JSON encoding
    headers         JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    published_at    TIMESTAMPTZ,
    attempt_count   INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX outbox_unpublished_idx
    ON outbox (next_attempt_at)
    WHERE published_at IS NULL;
```

Write path:

1. Begin the local transaction.
2. Apply the business change (for example insert the appointment).
3. Insert the outbox row with `published_at = NULL`.
4. Commit. Either both rows exist or neither does.

Relay path (`OutboxRelay` in Java, equivalent worker in Python):

1. Claim a batch with
   `SELECT ... FOR UPDATE SKIP LOCKED WHERE published_at IS NULL`.
2. Publish to Kafka with `acks=all`, idempotent producer, key = aggregate ID,
   and the stored headers.
3. Mark `published_at = now()` in the same database transaction.
4. On failure, increment `attempt_count`, apply exponential backoff to
   `next_attempt_at`, and retry. A row that exceeds the retry budget moves to
   the DLQ and raises an alert.

The same guarantee can be achieved with Debezium CDC on the outbox table
instead of polling. Polling is the local default because it has no additional
infrastructure; Debezium is the production option when outbox volume is high.
The event is at-least-once: a crash between publish and the `published_at`
update may publish twice. Consumers therefore deduplicate.

### 5.4 Idempotency

| Boundary | Key | Storage | Behaviour |
|---|---|---|---|
| Payment `POST /api/v1/payments` | `Idempotency-Key` header | `payment_idempotency` unique `(caller, key)` with request hash and stored response | Same key + same body replays the original response; same key + different body returns `409 IDEMPOTENCY_KEY_REUSED` |
| Invoice `POST /api/v1/invoices` | `idempotency_key` in `CreateInvoiceRequest` | `billing_idempotency` | Same as payments; prevents duplicate invoices on retry |
| Refund/void | Client-supplied idempotency key | payment/billing tables | Prevents double refund or double void |
| Kafka consumer | `eventId` from the envelope | `processed_events(event_id PRIMARY KEY, consumer_group, processed_at)` | Insert and business write commit together; duplicate insert means the event is skipped |
| Kafka producer | `enable.idempotence=true`, `acks=all` | Broker producer state | Prevents duplicate records caused by producer retries |

Consumer rule: never acknowledge an offset before the business change and the
`processed_events` row are committed. If the process dies after commit but
before ack, the event is redelivered and the unique key makes the second attempt
a no-op.

### 5.5 Optimistic locking in appointment-service

`appointment-service` is the concurrency hot spot: 10,000 active patients can
reach for the same slot in the same millisecond.

- `AppointmentSlot` has a JPA `@Version` column and a status
  (`AVAILABLE`, `HELD`, `BOOKED`, `BLOCKED`).
- Booking loads the slot, validates availability, and issues

  ```sql
  UPDATE appointment_slot
     SET status = 'BOOKED', appointment_id = :appointmentId, version = version + 1
   WHERE id = :slotId AND version = :expectedVersion AND status = 'AVAILABLE';
  ```

- If the update count is zero, the slot changed under the request. The service
  rolls back and returns
  `409 SLOT_ALREADY_BOOKED` with an `ErrorDetail`; the client refreshes the slot
  list. There is no blind retry against the same slot.
- A partial unique index on `(doctor_id, slot_start) WHERE status = 'BOOKED'`
  provides a database-level backstop, so a bug in the optimistic path cannot
  create two active appointments for the same slot.
- Rescheduling uses the same pattern: lock the new slot, then release the old
  slot in the same transaction.

The result is first-writer-wins with a clean client error instead of a lost
update or an overbooked clinic.

### 5.6 Encounter state machine

`encounter-service` models the visit explicitly. Transitions are validated
against the current state and persisted with `expected_state`/`version`; an
invalid transition returns `409 INVALID_ENCOUNTER_TRANSITION`. Every accepted
transition appends an `encounter_transition` audit row.

```
SCHEDULED ──check-in──▶ CHECKED_IN ──start──▶ IN_CONSULTATION
    │                       │                     │
    │                       │                     ├──order labs──▶ PENDING_LAB
    │                       │                     ├──dispense────▶ PENDING_PHARMACY
    │                       │                     └──ready───────▶ READY_FOR_DISCHARGE
    │                       │                                            │
    │                       │                                            └──discharge──▶ COMPLETED
    ├──no-show──────────────▶ NO_SHOW
    └──cancel───────────────▶ CANCELLED
```

| From | Allowed to | Guard |
|---|---|---|
| `SCHEDULED` | `CHECKED_IN`, `CANCELLED`, `NO_SHOW` | Appointment exists and belongs to the patient |
| `CHECKED_IN` | `IN_CONSULTATION`, `CANCELLED` | Triage may run before consultation but is not required |
| `IN_CONSULTATION` | `PENDING_LAB`, `PENDING_PHARMACY`, `READY_FOR_DISCHARGE` | At least one clinical note or order exists |
| `PENDING_LAB` | `IN_CONSULTATION`, `READY_FOR_DISCHARGE` | All ordered labs are verified, or the doctor waives them |
| `PENDING_PHARMACY` | `READY_FOR_DISCHARGE` | Prescriptions are dispensed or cancelled |
| `READY_FOR_DISCHARGE` | `COMPLETED` | Billing has enough charge data to assemble a draft invoice |
| `COMPLETED` | — (terminal) | Emits `EncounterCompleted` exactly once via the outbox |
| `CANCELLED`, `NO_SHOW` | — (terminal) | No invoice is generated; appointment slot is released |

The `EncounterCompleted` outbox row is written in the same transaction as the
`COMPLETED` transition. A unique constraint on
`(encounter_id, event_type)` in the outbox prevents a duplicate completion
event from retries or double submissions.

## 6. Saga: invoice, payment, inventory, insurance

There is no distributed transaction across billing, payment, pharmacy, and
insurance. The checkout flow is a **choreographed saga**: each service reacts
to an event, performs its local transaction, and publishes the next event. Every
forward action has a compensating action.

### 6.1 Happy path

1. **Encounter completed.** encounter-service commits `COMPLETED` and writes
   `EncounterCompleted` to its outbox.
2. **Invoice assembly.** billing-service consumes `EncounterCompleted`, loads
   charges (consultation, labs, drugs, bed), calls insurance-service for
   eligibility and the expected split, and creates an invoice in `PENDING`.
   It publishes `InvoiceIssued`.
3. **Notification/document.** notification-service sends the invoice notice;
   document-service renders the invoice PDF; insurance-service records/submits
   the claim for the insurer share.
4. **Payment attempt.** The patient or staff initiates payment through
   payment-service. payment-service writes a `PaymentAttempt` in
   `PROCESSING` with the idempotency key, calls the gateway, and records the
   result.
5. **Capture succeeds.** payment-service publishes `PaymentCompleted` and marks
   the attempt `CAPTURED`.
6. **Settlement.** billing-service consumes `PaymentCompleted`, applies the
   amount, and moves the invoice to `PAID` (or `PARTIALLY_PAID` for an
   instalment). document-service generates the receipt; notification-service
   sends the confirmation.
7. **Inventory finalisation.** pharmacy-inventory-service converts its
   reservation into a dispense record and decrements on-hand stock. If the
   medication was already handed out, this step is a no-op.

### 6.2 Failure path and compensation

Payment failure is a normal path, not an exception:

1. The gateway declines the charge. payment-service marks the attempt `FAILED`
   and publishes `PaymentFailed`.
2. billing-service consumes `PaymentFailed`, moves the invoice from
   `PAYMENT_PENDING` back to `PENDING`, and clears any temporary hold. It does
   **not** delete the invoice; the patient can retry with a new payment method.
3. pharmacy-inventory-service consumes `PaymentFailed`:
   - if stock is only **reserved** and not dispensed, release the reservation;
   - if stock was **dispensed**, it cannot be physically un-dispensed. The
     service writes a reconciliation record (return-to-stock when the item is
     returnable, otherwise a write-off) and keeps the charge. The invoice stays
     payable for the dispensed item.
4. notification-service sends a payment-failure message with a safe reason and
   the retry link.
5. document-service does not issue a paid receipt; it may render a
   "payment failed" notice.

If payment was captured but a later step fails (for example the receipt or
inventory finalisation), the saga compensates in reverse order:

| Forward step | Owner | Compensation | Idempotency |
|---|---|---|---|
| Issue invoice | billing-service | `VoidInvoice`; status `VOID`, reason recorded | Invoice ID + void key |
| Submit insurance claim | insurance-service | Cancel/void the claim; record the cancellation reference | Claim ID + idempotency key |
| Capture payment | payment-service | Refund the captured amount; status `REFUNDED` | Payment ID + refund key |
| Reserve stock | pharmacy-inventory-service | Release the reservation | Reservation ID |
| Dispense stock | pharmacy-inventory-service | Return-to-stock if returnable, otherwise write-off + audit record | Dispense ID |

Compensation events are produced through the same outbox. A compensation is
itself idempotent: replaying it must not issue a second refund or restock twice.
The saga is considered settled only when every participant has acknowledged the
forward action or its compensation; a background reconciler alerts on attempts
stuck in `PROCESSING` for more than five minutes.

### 6.3 Why not two-phase commit

The payment gateway and insurance clearinghouse are external systems and cannot
participate in XA. Kafka is not an XA participant either. 2PC would hold locks
across slow external calls and reduce availability during a partition. The saga
accepts temporary inconsistency and makes it observable and repairable; see
`docs/adr/0004-saga-over-two-phase-commit.md`.

## 7. Data architecture

### 7.1 Database-per-service

Each service is the sole owner of its database and schema. The only coupling
between services is an ID (patientId, encounterId, invoiceId) and the event
contract. Consequences:

- No cross-service SQL joins. A screen that needs data from several services
  is assembled by the gateway/BFF or by a service that subscribes to events and
  keeps a local read model.
- Referential integrity is local. A foreign key never points across databases.
  If patient-service deletes or merges a record, downstream services learn via
  an event rather than a cascading delete.
- Each service chooses its own persistence technology: PostgreSQL for
  transactional aggregates, MongoDB for the append-only EHR document shape,
  TimescaleDB for analytics time series, Redis for caching and distributed
  locks. This is intentional polyglot persistence.
- Migrations are owned per service: Flyway for Java services, Alembic for
  Python services. A migration only touches its own database.

### 7.2 Database names and engines

| Service | Engine | Logical database | Notes |
|---|---|---|---|
| auth-service | PostgreSQL | `hospital_auth` | Users, OTP challenges, refresh-token metadata; Keycloak uses `keycloak` |
| config-discovery-service | PostgreSQL | `hospital_config` | Config property source and registry metadata |
| patient-service | PostgreSQL | `hospital_patient` | Patients, insurance policies |
| appointment-service | PostgreSQL | `hospital_appointment` | Slots, appointments, outbox |
| doctor-service | PostgreSQL | `hospital_doctor` | Doctors, schedules, leave |
| department-service | PostgreSQL | `hospital_department` | Departments, rooms, beds, equipment |
| triage-service | PostgreSQL | `hospital_triage` | Triage records, ESI scores |
| encounter-service | PostgreSQL | `hospital_encounter` | Encounters, transition log, outbox |
| ehr-service | MongoDB | `hospital_ehr` | Append-only clinical documents, versions |
| prescription-service | PostgreSQL | `hospital_prescription` | Prescriptions, prescription lines |
| lab-service | PostgreSQL | `hospital_lab` | Orders, specimens, results |
| pharmacy-inventory-service | PostgreSQL | `hospital_pharmacy` | Stock, reservations, dispenses, alerts |
| billing-service | PostgreSQL | `hospital_billing` | Invoices, lines, idempotency, outbox |
| payment-service | PostgreSQL | `hospital_payment` | Payment attempts, refunds, idempotency, outbox |
| insurance-service | PostgreSQL | `hospital_insurance` | Policies, eligibility checks, claims |
| notification-service | PostgreSQL | `hospital_notification` | Notification log, templates, delivery status; Redis for Celery |
| analytics-service | TimescaleDB | `hospital_analytics` | Hypertables for waits, occupancy, revenue |
| ml-prediction-service | PostgreSQL | `hospital_ml` | Feature snapshots, model metadata, predictions |
| document-service | PostgreSQL + object storage | `hospital_document` | Document metadata; blobs in S3/MinIO |
| audit-service | PostgreSQL | `hospital_audit` | Append-only access and change log |
| api-gateway | Redis | — | Rate-limit counters and short-lived caches; no business data |

Local development may run one PostgreSQL container with one logical database
per service, plus a separate TimescaleDB container, MongoDB, and Redis. The
logical separation is the same as production even when the host is shared.

### 7.3 Data protection

- Encryption in transit (TLS) and at rest (managed-disk encryption or
  application-level encryption for selected columns).
- PII masking in logs. Helpers print `***` or a stable hash, never names,
  emails, phone numbers, national IDs, or free-text notes.
- Audit events are append-only and shipped to a separate store; the audit
  database has no update/delete API.
- Backups are per service, encrypted, and restore-tested. Because there are no
  cross-service foreign keys, a restore can be validated service by service.
- Data retention: clinical records follow the jurisdiction's retention policy;
  Kafka retains seven days of events for replay, with long-term archival
  handled separately.

## 8. Security and RBAC

### 8.1 Identity and token flow

1. The frontend redirects to Keycloak (OIDC authorization code + PKCE) or calls
   auth-service for registration/login/OTP.
2. Keycloak issues an access token (JWT) with `sub`, `realm_access.roles`,
   `tenant`, and expiry. Local development may use HS256 with a shared secret;
   production uses RS256 and a JWKS endpoint.
3. The API gateway validates signature, issuer, audience, and expiry, rejects
   missing/expired tokens with `401`, and forwards the token plus
   `X-Correlation-Id`.
4. Every service validates the token again (defence in depth) and enforces
   method-level RBAC. Internal service-to-service calls use client credentials
   with narrow scopes.

### 8.2 Role matrix

| Capability | Patient | Doctor | Nurse | Receptionist | Billing | Admin |
|---|---|---|---|---|---|---|
| Own profile / demographics | Read/write own | Read in care | Read in care | Read/write | Read billing-relevant | Full |
| Appointments | Own read/book/cancel | Read own schedule | Read unit schedule | Full | Read for billing | Full |
| Triage / vitals | — | Read | Write | Read | — | Full |
| Clinical notes / diagnoses | Read released own | Write/read | Read | — | — | Full |
| Prescriptions | Read own | Write/read | Read/administer | — | Read charge codes | Full |
| Labs | Read released own | Order/read | Read | — | Read charge codes | Full |
| Invoices / payments | Own read/pay | — | — | Collect payment | Full | Full |
| Insurance claims | Own read | — | — | — | Full | Full |
| Analytics dashboards | — | Own metrics | Unit metrics | Front-desk metrics | Revenue | Full |
| Audit trail | — | — | — | — | — | Full |
| User/role administration | — | — | — | — | — | Full |

Explicit rules:

- A nurse must not see billing data.
- Billing staff must not see clinical notes; they see charge codes and amounts.
- A patient may only read their own records; the gateway and services compare
  the token `sub`/patient binding and return `403` otherwise.
- Every read of a clinical record emits an audit event to audit-service with
  actor, patient ID, resource, action, timestamp, and correlation ID.

## 9. Observability

- **Tracing.** OpenTelemetry in both stacks. The gateway starts or continues a
  trace, injects W3C `traceparent`, and propagates `X-Correlation-Id`. A single
  trace follows React → gateway → Java service → Kafka → Python consumer.
  Spans are exported over OTLP to the collector.
- **Logs.** Structured JSON to stdout, collected by Loki. Every line includes
  `service`, `level`, `timestamp`, `correlationId`, `traceId`, `spanId`, and a
  PII-masked message. The Java logging pattern already includes the correlation
  ID via MDC.
- **Metrics.** Micrometer/Actuator for Java, Prometheus instrumentation for
  FastAPI. Dashboards cover request rate/errors/duration, JVM/GC, DB pools,
  Kafka consumer lag, outbox backlog, saga attempts stuck in `PROCESSING`, and
  business metrics (bookings, no-shows, revenue).
- **Health.** Every service exposes `/actuator/health` (Java) or `/health`
  (Python); readiness includes database and Kafka connectivity. Kubernetes uses
  these probes. The gateway exposes `/actuator/health` and `/actuator/gateway`.
- **Alerting.** Page on availability/SLO burn, consumer lag above threshold,
  outbox rows older than five minutes, DLQ growth, and payment-failure spikes.
- **Audit vs observability.** Audit logs are a compliance record and are not
  sampled. Observability data may be sampled; audit data may not.

## 10. Running locally

The repository is designed to run with Docker Compose.

```bash
# 1. Create local environment
cp .env.example .env

# 2. Start infrastructure only (Kafka, PostgreSQL, TimescaleDB, MongoDB,
#    Redis, Keycloak, schema registry)
make infra-up
# equivalent:
# docker compose -f docker-compose.infra.yml up -d

# 3. Generate code from contracts (optional; tools are skipped if absent)
make contracts

# 4. Start the full platform
make up
# equivalent:
# docker compose up -d --build

# 5. Inspect
make ps
make logs

# 6. Stop
make down
# stop infra only: make infra-down
```

Local endpoints:

| Component | URL / port |
|---|---|
| API gateway | `http://localhost:8080` |
| Keycloak | `http://localhost:8180` (realm `hospital`) |
| Service Eureka/Config | `http://localhost:8888` |
| PostgreSQL | `localhost:5432` |
| TimescaleDB | `localhost:5433` |
| Redis | `localhost:6379` |
| Kafka | `kafka:9092` inside Compose |
| Schema registry | `http://schema-registry:8081` inside Compose |
| Prometheus / Grafana / Loki | `9090` / `3000` / `3100` |

Typical local loop:

```bash
make test                 # Java + Python test suites
make test-java            # Maven tests, including Testcontainers
make test-python          # pytest per Python service
make contracts            # regenerate protobuf/Avro/Pydantic models
make build                # build all container images
```

## 11. API gateway routing

The gateway is the only public entry point. Routes are defined in
`services-java/api-gateway/src/main/java/com/hospital/gateway/config/RouteConfig.java`
and load-balanced through Eureka (`lb://`).

| Path prefix | Target service |
|---|---|
| `/api/v1/auth/**` | `auth-service` |
| `/api/v1/patients/**` | `patient-service` |
| `/api/v1/appointments/**`, `/api/v1/slots/**` | `appointment-service` |
| `/api/v1/doctors/**` | `doctor-service` |
| `/api/v1/departments/**`, `/api/v1/rooms/**`, `/api/v1/beds/**` | `department-service` |
| `/api/v1/encounters/**` | `encounter-service` |
| `/api/v1/ehr/**` | `ehr-service` |
| `/api/v1/prescriptions/**` | `prescription-service` |
| `/api/v1/labs/**` | `lab-service` |
| `/api/v1/pharmacy/**`, `/api/v1/stock/**` | `pharmacy-inventory-service` |
| `/api/v1/billing/**`, `/api/v1/invoices/**` | `billing-service` |
| `/api/v1/payments/**` | `payment-service` |
| `/api/v1/triage/**` | `triage-service` |
| `/api/v1/insurance/**` | `insurance-service` |
| `/api/v1/notifications/**` | `notification-service` |
| `/api/v1/analytics/**` | `analytics-service` |
| `/api/v1/predictions/**` | `ml-prediction-service` |
| `/api/v1/documents/**` | `document-service` |
| `/api/v1/audit/**` | `audit-service` |

Gateway responsibilities: JWT validation, role checks for coarse routes, CORS,
request/response size limits, per-client rate limiting, correlation ID
injection/propagation, and 30-second response timeout with 5-second connect
timeout. Business authorization remains in each service.

## 12. Ports reference

| Service | Host port | Protocol | Notes |
|---|---:|---|---|
| `api-gateway` | 8080 | HTTP | Public entry point |
| `auth-service` | 8081 | HTTP | Keycloak browser flow redirects to 8180 |
| `patient-service` | 8082 | HTTP/gRPC | gRPC on a separate configured port in production |
| `appointment-service` | 8083 | HTTP/gRPC | Slot queries are gRPC-ready |
| `doctor-service` | 8084 | HTTP | |
| `department-service` | 8085 | HTTP | |
| `encounter-service` | 8086 | HTTP | |
| `ehr-service` | 8087 | HTTP | |
| `prescription-service` | 8088 | HTTP | |
| `lab-service` | 8089 | HTTP | |
| `pharmacy-inventory-service` | 8090 | HTTP | |
| `billing-service` | 8091 | HTTP/gRPC | Invoice gRPC contract in `contracts/proto/billing.proto` |
| `payment-service` | 8092 | HTTP | |
| `triage-service` | 8001 | HTTP | Python |
| `insurance-service` | 8002 | HTTP | Python |
| `notification-service` | 8003 | HTTP | Python; Celery workers are internal |
| `analytics-service` | 8004 | HTTP | Python |
| `ml-prediction-service` | 8005 | HTTP | Python |
| `document-service` | 8006 | HTTP | Python |
| `audit-service` | 8007 | HTTP | Python |
| `config-discovery-service` | 8888 | HTTP | Eureka at `/eureka`, config at `/` |

## 13. Cross-language contracts

| Contract | Location | Generated into | Used for |
|---|---|---|---|
| Protobuf/gRPC | `contracts/proto/*.proto` | `services-java/shared-kernel/src/main/java` (`com.hospital.contracts.v1`) | Synchronous patient and billing queries |
| Avro | `contracts/avro/*.avsc` | Java records in `com.hospital.events`; Python models in `hospital_shared.events.generated` | Kafka event payloads |
| AsyncAPI | `docs/events/asyncapi.yaml` | Documentation | Topic, headers, producers, consumers |
| OpenAPI | each service `/v3/api-docs` | `docs/api/openapi/` | REST documentation and aggregation |

`contracts/codegen.sh` is idempotent and best-effort: `protoc`, `avro-tools`,
and `datamodel-codegen` are used when installed; missing tools are reported and
skipped without failing the build. A schema change is not complete until the
generated models compile in both stacks and the contract tests pass.

## 14. Failure modes and operational notes

| Failure | Detection | Response |
|---|---|---|
| Kafka unavailable | Producer/consumer health, outbox backlog | Keep writing to outbox; relay retries with backoff; API remains available for local writes |
| Consumer repeatedly fails | Retry count, DLQ depth | Route to `hospital.events.dlq`, alert, replay after fix |
| Duplicate event | `processed_events` unique key | Skip duplicate; log at debug |
| Slot race | `409 SLOT_ALREADY_BOOKED` | Client refreshes availability; no lost update |
| Payment gateway timeout | Attempt stuck in `PROCESSING` | Query gateway by idempotency key; reconcile; never charge twice |
| Saga stuck | Reconciler scans attempts older than five minutes | Alert; operator runs the documented compensation/retry |
| Cross-service data drift | Reconciliation jobs on event counts and read models | Replay events or rebuild the read model |
| PII in logs | Log scanning / policy tests | Redact, rotate, and add a regression test |

Phase one deliberately excludes inpatient admission, theatre scheduling, FHIR
interoperability, PACS/DICOM, GDPR data-subject automation, and physical queue
displays. The event and state-machine boundaries are chosen so those flows can
be added without rewriting the existing ones.
