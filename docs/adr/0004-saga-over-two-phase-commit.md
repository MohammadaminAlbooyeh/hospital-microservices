# 0004 — Choreographed saga over two-phase commit

Status: Accepted

## Context

Step 10–11 of the patient journey (`README.md` §4) spans four services that
each own their own database: `billing-service` assembles the invoice,
`insurance-service` submits the claim and determines the patient share,
`payment-service` captures the charge, and `pharmacy-inventory-service`
finalises the dispense. Per `0003-database-per-service.md` these cannot share
one ACID transaction — there is no database boundary that spans all four.

`docs/architecture.md` §5 names this directly: "Payment, invoicing and
inventory cannot live inside one ACID transaction." Two designs were
considered:

1. **Two-phase commit (2PC)**, coordinating a distributed transaction across
   billing, payment, insurance, and inventory with a transaction manager
   holding locks until every participant votes to commit.
2. **Choreographed saga**, where each service reacts to an event, performs
   its own local transaction, and publishes the next event — with an explicit
   compensating action for every forward action.

The payment gateway and the insurance clearinghouse are both **external
systems** reached over the network; they cannot participate in an XA
transaction at all. Kafka — the event backbone — is not an XA participant
either (see `0002-kafka-vs-rabbitmq.md`). Any 2PC design would therefore have
to exclude the two steps that most need coordination, which defeats the
purpose.

Even ignoring that, 2PC's locking behaviour is the wrong trade for this
workload. A payment gateway call (README §4 step 11) can legitimately take
seconds, decline, or time out. Holding locks on the invoice and inventory rows
across that call — the SQL Server / XA pattern — means a slow or unavailable
external gateway makes the whole checkout path unavailable for every patient,
not just the one whose card was declined.

## Decision

The checkout flow is a **choreographed saga** (`docs/architecture.md` §6),
not an orchestrated one and not 2PC:

1. `encounter-service` commits `COMPLETED` and emits `EncounterCompleted`.
2. `billing-service` consumes it, assembles the invoice, calls
   `insurance-service` synchronously for eligibility/split, and emits
   `InvoiceIssued`.
3. `payment-service` processes the payment attempt independently, keyed by an
   idempotency key, and emits `PaymentCompleted` or `PaymentFailed`.
4. Each downstream participant reacts to the outcome with its own local
   transaction and, where the forward action needs to be undone, a named
   compensating action — see the compensation table in
   `docs/architecture.md` §6.2 (`VoidInvoice`, claim cancellation, refund,
   reservation release, return-to-stock/write-off).

The **payment failure path is the designed case, not an exception path**
(README §4 step 11, `docs/architecture.md` §6.2): `PaymentFailed` reverts the
invoice from `PAYMENT_PENDING` to `PENDING` (never deleted — the patient can
retry with a new method), releases or reconciles pharmacy stock depending on
whether it was only reserved or already dispensed, and notifies the patient
with a safe reason and a retry link. If payment succeeds but a later step
fails, the saga compensates in reverse order (refund the capture, void the
claim, void the invoice) using the same outbox and the same idempotency
discipline as the forward path.

## Consequences

- The system runs with **temporary inconsistency** by design: an invoice can
  briefly be `PENDING` while the claim is mid-flight, or `PAYMENT_PENDING`
  while waiting on the gateway. This is accepted and made observable — a
  background reconciler alerts on any saga attempt stuck in `PROCESSING` for
  more than five minutes (`docs/architecture.md` §6.2, §9).
- Every forward action must have a compensating action that is itself
  idempotent; replaying a compensation must not issue a second refund or
  restock twice. This roughly doubles the state machine surface each
  participant has to implement compared to a simple happy path.
- There is no single coordinator to debug — the saga's state is implicit in
  the union of each participant's local state plus the event log. Kafka's
  durability and replay (`0002-kafka-vs-rabbitmq.md`) are what make this
  debuggable at all: a stuck saga can be diagnosed by replaying the event
  history for one `invoiceId`/`paymentId`.
- Availability is preserved during partial outages: a slow or down payment
  gateway blocks only the payment step, not the ability to keep registering
  patients, booking appointments, or recording lab results elsewhere in the
  system — which a lock-holding 2PC design across these services would not
  guarantee.
