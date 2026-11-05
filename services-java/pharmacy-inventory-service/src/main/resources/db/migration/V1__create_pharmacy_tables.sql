CREATE TABLE IF NOT EXISTS drugs (
    id              UUID PRIMARY KEY,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    code            VARCHAR(64)   NOT NULL UNIQUE,
    name            VARCHAR(255)  NOT NULL,
    form            VARCHAR(128),
    unit            VARCHAR(64),
    unit_price      NUMERIC(12,2) NOT NULL DEFAULT 0,
    quantity_on_hand INTEGER      NOT NULL DEFAULT 0,
    reorder_level   INTEGER       NOT NULL DEFAULT 0,
    expiry_date     DATE
);

CREATE INDEX IF NOT EXISTS idx_drugs_code ON drugs (code);
CREATE INDEX IF NOT EXISTS idx_drugs_expiry ON drugs (expiry_date);

CREATE TABLE IF NOT EXISTS stock_movements (
    id          UUID PRIMARY KEY,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    drug_code   VARCHAR(64)  NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    quantity    INTEGER      NOT NULL,
    reference   VARCHAR(512) UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_drug ON stock_movements (drug_code);
CREATE INDEX IF NOT EXISTS idx_stock_movements_type ON stock_movements (type);

CREATE TABLE IF NOT EXISTS dispense_records (
    id              UUID PRIMARY KEY,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    patient_id      UUID         NOT NULL,
    encounter_id    UUID,
    prescription_id UUID,
    drug_code       VARCHAR(64)  NOT NULL,
    quantity        INTEGER      NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    finalized_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_dispense_records_patient ON dispense_records (patient_id);
CREATE INDEX IF NOT EXISTS idx_dispense_records_status ON dispense_records (status);
CREATE INDEX IF NOT EXISTS idx_dispense_records_key ON dispense_records (idempotency_key);
