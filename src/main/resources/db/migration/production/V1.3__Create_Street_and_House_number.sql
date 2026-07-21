CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =========================
-- STREET
-- =========================

CREATE TABLE IF NOT EXISTS street (
                                              id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    city_id      UUID NOT NULL
    REFERENCES city(id)
    ON DELETE CASCADE,

    name         TEXT NOT NULL,
    location     GEOGRAPHY(Point, 4326),

    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Case-insensitive uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS uq_street_city_name_ci
    ON street (city_id, lower(name));

CREATE INDEX IF NOT EXISTS idx_street_city
    ON street (city_id);

CREATE INDEX IF NOT EXISTS idx_street_name_trgm
    ON street USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_street_location
    ON street USING GIST (location);


-- =========================
-- HOUSE_NUMBER
-- =========================

CREATE TABLE IF NOT EXISTS house_number (
                                                    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    street_id    UUID NOT NULL
    REFERENCES street(id)
    ON DELETE CASCADE,

    number       VARCHAR(20) NOT NULL,
    location     GEOGRAPHY(Point, 4326),

    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Case-insensitive uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS uq_house_number_ci
    ON house_number (street_id, lower(number));

CREATE INDEX IF NOT EXISTS idx_house_number_street
    ON house_number (street_id);

CREATE INDEX IF NOT EXISTS idx_house_number_location
    ON house_number USING GIST (location);

CREATE INDEX IF NOT EXISTS idx_house_number_number_trgm
    ON house_number USING GIN (number gin_trgm_ops);