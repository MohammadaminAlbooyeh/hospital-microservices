CREATE TABLE IF NOT EXISTS appointments (
    id              UUID PRIMARY KEY,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    patient_id      UUID         NOT NULL,
    doctor_id       UUID         NOT NULL,
    department_id   UUID,
    slot_start      TIMESTAMPTZ  NOT NULL,
    slot_end        TIMESTAMPTZ,
    status          VARCHAR(32)  NOT NULL DEFAULT 'BOOKED',
    reason          TEXT,
    cancelled_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments (patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor ON appointments (doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointments_slot_start ON appointments (slot_start);

-- Prevent double-booking: a doctor may only have one active (non-cancelled)
-- appointment per exact slot start. Acts as a DB-level safety net on top of
-- the optimistic @Version lock used by the application layer.
CREATE UNIQUE INDEX IF NOT EXISTS uq_appointments_doctor_slot_active
    ON appointments (doctor_id, slot_start)
    WHERE status <> 'CANCELLED';
