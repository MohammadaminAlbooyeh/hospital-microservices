CREATE TABLE IF NOT EXISTS patients (
    id                      UUID PRIMARY KEY,
    version                 BIGINT       NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    mrn                     VARCHAR(255) UNIQUE,
    first_name              VARCHAR(255) NOT NULL,
    last_name               VARCHAR(255) NOT NULL,
    date_of_birth           DATE         NOT NULL,
    gender                  VARCHAR(32)  NOT NULL,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    phone                   VARCHAR(64)  NOT NULL,
    address_line            VARCHAR(512),
    city                    VARCHAR(128),
    country                 VARCHAR(128),
    national_id             VARCHAR(128) NOT NULL UNIQUE,
    allergies               TEXT,
    emergency_contact_name  VARCHAR(255),
    emergency_contact_phone VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_patients_name ON patients (last_name, first_name);
