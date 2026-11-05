CREATE TABLE IF NOT EXISTS payments (
    id                  UUID PRIMARY KEY,
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    invoice_id          UUID          NOT NULL,
    patient_id          UUID,
    amount              NUMERIC(19,2) NOT NULL,
    currency            VARCHAR(8)    NOT NULL,
    method              VARCHAR(32)   NOT NULL,
    status              VARCHAR(32)   NOT NULL,
    idempotency_key     VARCHAR(255)  NOT NULL UNIQUE,
    gateway_reference   VARCHAR(255),
    failure_reason      VARCHAR(1024),
    processed_at        TIMESTAMPTZ,
    refunded_at         TIMESTAMPTZ,
    refund_amount       NUMERIC(19,2)
);

CREATE INDEX IF NOT EXISTS idx_payments_invoice ON payments (invoice_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_patient ON payments (patient_id);

CREATE TABLE IF NOT EXISTS outbox (
    id              VARCHAR(255) PRIMARY KEY,
    aggregate_type  VARCHAR(255) NOT NULL,
    aggregate_id    VARCHAR(255) NOT NULL,
    event_type      VARCHAR(255) NOT NULL,
    payload         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_published ON outbox (published);
