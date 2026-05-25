-- =====================================================
-- STATE
-- =====================================================

CREATE TABLE IF NOT EXISTS state (
                                             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    country_id      UUID NOT NULL
    REFERENCES country(id)
    ON DELETE CASCADE,

    code            VARCHAR(20) NOT NULL,          -- BDS
    iso3166_2       VARCHAR(20),                   -- AF-BDS

    name            VARCHAR(200) NOT NULL,

    type            VARCHAR(100),                  -- province, state, region
    level           INTEGER,                       -- hierarchy level
    parent_id       UUID
    REFERENCES state(id)
    ON DELETE SET NULL,

    latitude        DECIMAL(10,8),
    longitude       DECIMAL(11,8),

    population      BIGINT,

    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_state_country_code
    UNIQUE (country_id, code)
    );

-- =====================================================
-- INDEXES
-- =====================================================

-- Fast lookup by country
CREATE INDEX IF NOT EXISTS idx_state_country
    ON state(country_id);

-- Hierarchy
CREATE INDEX IF NOT EXISTS idx_state_parent
    ON state(parent_id);

-- ISO lookup
CREATE INDEX IF NOT EXISTS idx_state_iso3166
    ON state(iso3166_2);

-- Name search (requires pg_trgm extension)
CREATE INDEX IF NOT EXISTS idx_state_name_trgm
    ON state
    USING GIN (name gin_trgm_ops);

-- Optional but recommended for filters
CREATE INDEX IF NOT EXISTS idx_state_code
    ON state(code);

-- =====================================================
-- STATE ↔ TIMEZONE (ManyToMany)
-- =====================================================

CREATE TABLE IF NOT EXISTS state_timezone (
                                                      state_id       UUID NOT NULL
                                                      REFERENCES state(id)
    ON DELETE CASCADE,

    timezone_id    UUID NOT NULL
    REFERENCES timezone(id)
    ON DELETE CASCADE,

    PRIMARY KEY (state_id, timezone_id)
    );

CREATE INDEX IF NOT EXISTS idx_state_timezone_state
    ON state_timezone(state_id);

CREATE INDEX IF NOT EXISTS idx_state_timezone_timezone
    ON state_timezone(timezone_id);