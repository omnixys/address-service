CREATE TABLE IF NOT EXISTS city (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state_id    UUID NOT NULL REFERENCES state(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    location    GEOGRAPHY(Point, 4326),
    population  BIGINT,
    timezone_id UUID REFERENCES timezone(id) ON DELETE SET NULL,
    type        VARCHAR(50),
    level       INTEGER,
    parent_id   UUID
    REFERENCES city(id) ON DELETE SET NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_city_population_not_null ON city (population DESC) WHERE population IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_city_location ON city USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_city_name_trgm ON city USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_city_parent ON city(parent_id);
CREATE INDEX IF NOT EXISTS idx_city_timezone ON city (timezone_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_city_state_name_ci ON city (state_id, lower(name));
CREATE INDEX IF NOT EXISTS idx_city_state ON city (state_id);


ALTER TABLE country ADD COLUMN IF NOT EXISTS capital_city_id UUID REFERENCES city(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS postal_code (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id  UUID NOT NULL REFERENCES country(id) ON DELETE CASCADE,
    city_id     UUID NOT NULL REFERENCES city(id) ON DELETE CASCADE,
    code         VARCHAR(20) NOT NULL,
    location    GEOGRAPHY(Point, 4326),
    accuracy    INTEGER,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_postal UNIQUE (country_id, city_id, code)
    );

CREATE INDEX IF NOT EXISTS idx_postal_code_country ON postal_code (country_id, code);
CREATE INDEX IF NOT EXISTS idx_postal_city ON postal_code (city_id);
CREATE INDEX IF NOT EXISTS idx_postal_country ON postal_code (country_id);
CREATE INDEX IF NOT EXISTS idx_postal_location ON postal_code USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_postal_code_trgm ON postal_code USING GIN (code gin_trgm_ops);