-- =========================
-- STREET
-- =========================

CREATE TABLE IF NOT EXISTS event_address (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id      UUID NOT NULL UNIQUE,

    country_id      UUID NOT NULL
    REFERENCES country(id)
        ON DELETE RESTRICT,

    state_id      UUID
    REFERENCES state(id)
        ON DELETE SET NULL,

    city_id      UUID NOT NULL
    REFERENCES city(id)
        ON DELETE RESTRICT,

    postal_code_id      UUID
    REFERENCES postal_code(id)
        ON DELETE SET NULL,

    street_id      UUID NOT NULL
    REFERENCES street(id)
        ON DELETE RESTRICT,

    house_number_id      UUID NOT NULL
    REFERENCES house_number(id)
        ON DELETE RESTRICT,

    additional_info VARCHAR(50),

    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- =========================
-- INDEXES
-- =========================

CREATE INDEX IF NOT EXISTS idx_event_address_event
    ON event_address (event_id);

CREATE INDEX IF NOT EXISTS idx_event_address_city
    ON event_address (city_id);

CREATE INDEX IF NOT EXISTS idx_event_address_postal
    ON event_address (postal_code_id);

CREATE INDEX IF NOT EXISTS idx_event_address_country
    ON event_address (country_id);