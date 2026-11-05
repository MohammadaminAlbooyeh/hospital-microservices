CREATE TABLE IF NOT EXISTS insurance_policies (
    id               UUID PRIMARY KEY,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    provider         VARCHAR(255) NOT NULL,
    policy_number    VARCHAR(255) NOT NULL UNIQUE,
    member_id        VARCHAR(255) NOT NULL,
    valid_from       DATE,
    valid_to         DATE,
    coverage_percent INTEGER
);

ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS insurance_policy_id UUID;

ALTER TABLE patients
    DROP CONSTRAINT IF EXISTS fk_patients_insurance;

ALTER TABLE patients
    ADD CONSTRAINT fk_patients_insurance
        FOREIGN KEY (insurance_policy_id) REFERENCES insurance_policies (id);

CREATE INDEX IF NOT EXISTS idx_insurance_provider ON insurance_policies (provider);
