CREATE TABLE IF NOT EXISTS invoices (
    id                          UUID PRIMARY KEY,
    version                     BIGINT        NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ   NOT NULL,
    updated_at                  TIMESTAMPTZ   NOT NULL,
    encounter_id                UUID,
    patient_id                  UUID          NOT NULL,
    status                      VARCHAR(32)   NOT NULL,
    currency                    VARCHAR(8)    NOT NULL,
    subtotal                    NUMERIC(19,2) NOT NULL DEFAULT 0,
    insurer_share               NUMERIC(19,2) NOT NULL DEFAULT 0,
    patient_share               NUMERIC(19,2) NOT NULL DEFAULT 0,
    total_amount                NUMERIC(19,2) NOT NULL DEFAULT 0,
    insurance_coverage_percent  NUMERIC(5,2)  NOT NULL DEFAULT 0,
    issued_at                   TIMESTAMPTZ,
    paid_at                     TIMESTAMPTZ,
    failure_reason              VARCHAR(1024),
    last_payment_id             UUID
);

CREATE INDEX IF NOT EXISTS idx_invoices_patient ON invoices (patient_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices (status);
CREATE INDEX IF NOT EXISTS idx_invoices_encounter ON invoices (encounter_id);

CREATE TABLE IF NOT EXISTS invoice_lines (
    id           UUID PRIMARY KEY,
    invoice_id   UUID          NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    category     VARCHAR(32)   NOT NULL,
    description  VARCHAR(512)  NOT NULL,
    quantity     INTEGER       NOT NULL,
    unit_price   NUMERIC(19,2) NOT NULL,
    amount       NUMERIC(19,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_invoice_lines_invoice ON invoice_lines (invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_lines_category ON invoice_lines (category);

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
