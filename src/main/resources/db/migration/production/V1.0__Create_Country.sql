
CREATE TABLE IF NOT EXISTS continent (
                                                 id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    code        VARCHAR(10) UNIQUE, -- EU, AF, NA, SA, AS, OC, AN
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
CREATE INDEX IF NOT EXISTS idx_continent_code ON continent(code);


CREATE TABLE IF NOT EXISTS subregion (
                                                 id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(150) NOT NULL,
    continent_id  UUID NOT NULL REFERENCES continent(id) ON DELETE RESTRICT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_subregion UNIQUE (name, continent_id)
    );
CREATE INDEX IF NOT EXISTS idx_subregion_continent ON subregion(continent_id);


CREATE TABLE IF NOT EXISTS currency (
                                                id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    code        CHAR(3) NOT NULL UNIQUE,
    symbol      VARCHAR(10),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );


CREATE TABLE IF NOT EXISTS language (
                                                id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    iso2        CHAR(2) UNIQUE,
    iso3        CHAR(3) UNIQUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );


CREATE TABLE IF NOT EXISTS timezone (
                                                id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    zone_name       VARCHAR(150) NOT NULL UNIQUE, -- Europe/Berlin
    gmt_offset      INTEGER NOT NULL,             -- seconds (3600)
    gmt_offset_name VARCHAR(20) NOT NULL,         -- UTC+01:00
    abbreviation    VARCHAR(10),                  -- CET
    tz_name         VARCHAR(150),                 -- Central European Time

    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
CREATE INDEX IF NOT EXISTS idx_timezone_zone_name ON timezone(zone_name);


CREATE TABLE IF NOT EXISTS calling_code (
                                                    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(10) NOT NULL UNIQUE, -- +49
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );


CREATE TABLE IF NOT EXISTS country (
                                               id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name          TEXT NOT NULL,
    iso2          CHAR(2) NOT NULL UNIQUE,
    iso3          CHAR(3) NOT NULL UNIQUE,
    numeric_code CHAR(3) UNIQUE,

    flag_svg      TEXT,
    flag_png      TEXT,

    nationality   VARCHAR(200),
    tld           VARCHAR(10),

    population    BIGINT,
    area_sq_km    double precision,

    latitude      DECIMAL(10,8),
    longitude     DECIMAL(11,8),

    currency_id UUID REFERENCES currency(id),
    calling_code_id UUID REFERENCES calling_code(id),

    continent_id  UUID NOT NULL REFERENCES continent(id) ON DELETE RESTRICT,
    subregion_id  UUID NOT NULL REFERENCES subregion(id) ON DELETE RESTRICT,

    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
CREATE INDEX IF NOT EXISTS idx_country_continent ON country(continent_id);
CREATE INDEX IF NOT EXISTS idx_country_subregion ON country(subregion_id);


CREATE TABLE IF NOT EXISTS country_timezone (
                                                        country_id  UUID REFERENCES country(id) ON DELETE CASCADE,
    timezone_id UUID REFERENCES timezone(id) ON DELETE CASCADE,
    PRIMARY KEY (country_id, timezone_id)
    );


CREATE TABLE IF NOT EXISTS country_language (
                                                        country_id  UUID REFERENCES country(id) ON DELETE CASCADE,
    language_id UUID REFERENCES language(id) ON DELETE CASCADE,
    PRIMARY KEY (country_id, language_id)
    );
