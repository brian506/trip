

CREATE TABLE IF NOT EXISTS stay (
    stay_id         UUID         NOT NULL,
    supplier        VARCHAR(16)  NOT NULL,
    stay_code       VARCHAR(64)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT pk_stay PRIMARY KEY (stay_id),
    CONSTRAINT stay_supplier_code UNIQUE (supplier, stay_code)
);

CREATE INDEX IF NOT EXISTS idx_stay_supplier_active ON stay (supplier, active);

CREATE TABLE IF NOT EXISTS room_type (
    room_type_id    UUID         NOT NULL,
    stay_id         UUID         NOT NULL,
    room_type_code  VARCHAR(64)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    max_occupancy   INT          NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT pk_room_type PRIMARY KEY (room_type_id),
    CONSTRAINT room_type_stay_code UNIQUE (stay_id, room_type_code),
    CONSTRAINT fk_room_type_stay FOREIGN KEY (stay_id) REFERENCES stay (stay_id)
);

CREATE INDEX IF NOT EXISTS idx_room_type_stay ON room_type (stay_id);
