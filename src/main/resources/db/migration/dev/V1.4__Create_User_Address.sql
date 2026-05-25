CREATE TYPE address_type AS ENUM (
    'HOME',
    'WORK',
    'SHIPPING',
    'BILLING'
    );

-- =========================
-- STREET
-- =========================

CREATE TABLE IF NOT EXISTS user_address (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,

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
    address_type address_type NOT NULL,

    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- =========================
-- INDEXES
-- =========================

CREATE INDEX IF NOT EXISTS idx_user_address_user
    ON user_address (user_id);

CREATE INDEX IF NOT EXISTS idx_user_address_city
    ON user_address (city_id);

CREATE INDEX IF NOT EXISTS idx_user_address_postal
    ON user_address (postal_code_id);

CREATE INDEX IF NOT EXISTS idx_user_address_country
    ON user_address (country_id);

CREATE UNIQUE INDEX uq_user_home_address
    ON user_address(user_id)
    WHERE address_type = 'HOME';