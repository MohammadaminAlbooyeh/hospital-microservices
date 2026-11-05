CREATE TABLE IF NOT EXISTS departments (
    id          UUID PRIMARY KEY,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    name        VARCHAR(255) NOT NULL UNIQUE,
    floor       VARCHAR(64),
    phone       VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS rooms (
    id            UUID PRIMARY KEY,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    department_id UUID         NOT NULL REFERENCES departments (id) ON DELETE CASCADE,
    room_number   VARCHAR(64)  NOT NULL,
    room_type     VARCHAR(64)  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rooms_department ON rooms (department_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_rooms_department_number ON rooms (department_id, room_number);

CREATE TABLE IF NOT EXISTS beds (
    id          UUID PRIMARY KEY,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    room_id     UUID         NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    bed_number  VARCHAR(64)  NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'AVAILABLE'
);

CREATE INDEX IF NOT EXISTS idx_beds_room ON beds (room_id);
CREATE INDEX IF NOT EXISTS idx_beds_status ON beds (status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_beds_room_number ON beds (room_id, bed_number);

CREATE TABLE IF NOT EXISTS equipment (
    id            UUID PRIMARY KEY,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    department_id UUID         NOT NULL REFERENCES departments (id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    quantity      INTEGER      NOT NULL DEFAULT 1,
    status        VARCHAR(32)  NOT NULL DEFAULT 'OPERATIONAL'
);

CREATE INDEX IF NOT EXISTS idx_equipment_department ON equipment (department_id);
