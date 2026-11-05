CREATE TABLE IF NOT EXISTS lab_orders (
    id            UUID PRIMARY KEY,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    patient_id    UUID         NOT NULL,
    encounter_id  UUID,
    test_code     VARCHAR(64)  NOT NULL,
    test_name     VARCHAR(255) NOT NULL,
    priority      VARCHAR(32)  NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    ordered_at    TIMESTAMPTZ  NOT NULL,
    ordered_by    VARCHAR(255) NOT NULL,
    notes         TEXT
);

CREATE INDEX IF NOT EXISTS idx_lab_orders_patient ON lab_orders (patient_id);
CREATE INDEX IF NOT EXISTS idx_lab_orders_status ON lab_orders (status);
CREATE INDEX IF NOT EXISTS idx_lab_orders_ordered_at ON lab_orders (ordered_at);

CREATE TABLE IF NOT EXISTS specimens (
    id           UUID PRIMARY KEY,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    order_id     UUID         NOT NULL,
    barcode      VARCHAR(128) NOT NULL UNIQUE,
    collected_at TIMESTAMPTZ  NOT NULL,
    collected_by VARCHAR(255) NOT NULL,
    status       VARCHAR(32)  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_specimens_order ON specimens (order_id);

CREATE TABLE IF NOT EXISTS lab_results (
    id              UUID PRIMARY KEY,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    order_id        UUID         NOT NULL UNIQUE,
    value           VARCHAR(255) NOT NULL,
    unit            VARCHAR(64),
    reference_range VARCHAR(255),
    critical        BOOLEAN      NOT NULL DEFAULT FALSE,
    result_at       TIMESTAMPTZ  NOT NULL,
    notes           TEXT
);

CREATE INDEX IF NOT EXISTS idx_lab_results_order ON lab_results (order_id);
