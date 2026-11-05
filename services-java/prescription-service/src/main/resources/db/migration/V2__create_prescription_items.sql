CREATE TABLE IF NOT EXISTS prescription_items (
    id              UUID PRIMARY KEY,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    prescription_id UUID        NOT NULL,
    drug_code       VARCHAR(128) NOT NULL,
    drug_name       VARCHAR(255) NOT NULL,
    dose            VARCHAR(128),
    frequency       VARCHAR(128),
    duration_days   INTEGER,
    instructions    TEXT,
    CONSTRAINT fk_prescription_items_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_prescription_items_prescription ON prescription_items (prescription_id);
