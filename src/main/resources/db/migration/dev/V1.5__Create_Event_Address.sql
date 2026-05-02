-- =========================
-- STREET
-- =========================

CREATE TABLE IF NOT EXISTS address.event_address (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id      UUID NOT NULL UNIQUE,

    country_id      UUID NOT NULL
    REFERENCES address.country(id)
        ON DELETE RESTRICT,

    state_id      UUID
    REFERENCES address.state(id)
        ON DELETE SET NULL,

    city_id      UUID NOT NULL
    REFERENCES address.city(id)
        ON DELETE RESTRICT,

    postal_code_id      UUID
    REFERENCES address.postal_code(id)
        ON DELETE SET NULL,

    street_id      UUID NOT NULL
    REFERENCES address.street(id)
        ON DELETE RESTRICT,

    house_number_id      UUID NOT NULL
    REFERENCES address.house_number(id)
        ON DELETE RESTRICT,

    additional_info VARCHAR(50),

    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- =========================
-- INDEXES
-- =========================

CREATE INDEX IF NOT EXISTS idx_event_address_event
    ON address.event_address (event_id);

CREATE INDEX IF NOT EXISTS idx_event_address_city
    ON address.event_address (city_id);

CREATE INDEX IF NOT EXISTS idx_event_address_postal
    ON address.event_address (postal_code_id);

CREATE INDEX IF NOT EXISTS idx_event_address_country
    ON address.event_address (country_id);