CREATE TABLE IF NOT EXISTS encounters (
    id               UUID PRIMARY KEY,
    version          BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    patient_id       UUID        NOT NULL,
    doctor_id        UUID,
    department_id    UUID,
    state            VARCHAR(32) NOT NULL,
    chief_complaint  TEXT,
    checked_in_at    TIMESTAMPTZ,
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    cancelled_at     TIMESTAMPTZ,
    cancel_reason    TEXT,
    diagnoses        TEXT
);

CREATE INDEX IF NOT EXISTS idx_encounters_patient ON encounters (patient_id);
CREATE INDEX IF NOT EXISTS idx_encounters_doctor ON encounters (doctor_id);
CREATE INDEX IF NOT EXISTS idx_encounters_state ON encounters (state);
CREATE INDEX IF NOT EXISTS idx_encounters_created ON encounters (created_at DESC);
