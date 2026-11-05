CREATE TABLE IF NOT EXISTS prescriptions (
    id            UUID PRIMARY KEY,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    patient_id    UUID        NOT NULL,
    encounter_id  UUID,
    doctor_id     UUID        NOT NULL,
    status        VARCHAR(32) NOT NULL,
    notes         TEXT,
    issued_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_patient ON prescriptions (patient_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_encounter ON prescriptions (encounter_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_status ON prescriptions (status);
