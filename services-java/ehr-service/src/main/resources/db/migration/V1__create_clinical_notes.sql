CREATE TABLE IF NOT EXISTS clinical_notes (
    id              UUID PRIMARY KEY,
    entity_version  BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    patient_id      UUID        NOT NULL,
    encounter_id    UUID,
    author_id       UUID,
    version         INTEGER     NOT NULL,
    content         TEXT        NOT NULL,
    diagnoses       TEXT,
    supersedes_id   UUID,
    CONSTRAINT uk_clinical_notes_patient_version UNIQUE (patient_id, version)
);

CREATE INDEX IF NOT EXISTS idx_clinical_notes_patient ON clinical_notes (patient_id, version DESC);
CREATE INDEX IF NOT EXISTS idx_clinical_notes_encounter ON clinical_notes (encounter_id);
