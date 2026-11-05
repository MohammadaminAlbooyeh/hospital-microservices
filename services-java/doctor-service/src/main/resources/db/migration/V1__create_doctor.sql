CREATE TABLE IF NOT EXISTS doctors (
    id              UUID PRIMARY KEY,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    speciality      VARCHAR(128) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(64)  NOT NULL,
    license_number  VARCHAR(128) NOT NULL UNIQUE,
    department_id   UUID,
    daily_capacity  INTEGER      NOT NULL DEFAULT 20,
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_doctors_speciality ON doctors (speciality);

CREATE TABLE IF NOT EXISTS doctor_schedules (
    id              UUID PRIMARY KEY,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    doctor_id       UUID         NOT NULL REFERENCES doctors (id) ON DELETE CASCADE,
    day_of_week     VARCHAR(16)  NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    max_appointments INTEGER     NOT NULL DEFAULT 10
);

CREATE INDEX IF NOT EXISTS idx_doctor_schedules_doctor ON doctor_schedules (doctor_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_doctor_schedule_day ON doctor_schedules (doctor_id, day_of_week);

CREATE TABLE IF NOT EXISTS doctor_leaves (
    id              UUID PRIMARY KEY,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    doctor_id       UUID         NOT NULL REFERENCES doctors (id) ON DELETE CASCADE,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    reason          VARCHAR(512)
);

CREATE INDEX IF NOT EXISTS idx_doctor_leaves_doctor ON doctor_leaves (doctor_id);
