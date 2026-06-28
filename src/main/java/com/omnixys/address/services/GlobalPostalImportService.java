package com.omnixys.address.services;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import tools.jackson.core.type.TypeReference;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalPostalImportService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Transactional
    public void importAll() throws Exception {

        var citiesJson = new ClassPathResource("data/cities.json");
        var geoNamesTxt = new ClassPathResource("data/allCountries.txt");

        Connection connection = DataSourceUtils.getConnection(dataSource);
        connection.setAutoCommit(false);

        try {

            importCitiesJson(connection, citiesJson);
            mergeCitiesFromJson(connection);

            importGeoNamesCity(connection, geoNamesTxt);
//            mergeStatesFromCity(connection);
            mergePostalCodes(connection);

            dropStagingTables(connection);

            connection.commit();
            log.info("Combined import completed.");

        } catch (Exception ex) {
            connection.rollback();
            throw ex;
        }
    }

    private void importCitiesJson(Connection connection, ClassPathResource resource) throws Exception {

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS staging_city_json");

            stmt.execute("""
            CREATE TABLE staging_city_json (
                country_code VARCHAR(2),
                state_code   VARCHAR(20),
                name         TEXT,
                latitude     DOUBLE PRECISION,
                longitude    DOUBLE PRECISION,
                population   BIGINT,
                timezone     VARCHAR(150),
                type         VARCHAR(50),
                level        INTEGER
            )
        """);
        }

        List<Map<String,Object>> cities =
                objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});

        StringBuilder sb = new StringBuilder();

        for (Map<String,Object> city : cities) {

            sb.append(val(city.get("country_code"))).append("\t")
                    .append(val(city.get("state_code"))).append("\t")
                    .append(val(city.get("name"))).append("\t")
                    .append(val(city.get("latitude"))).append("\t")
                    .append(val(city.get("longitude"))).append("\t")
                    .append(val(city.get("population"))).append("\t")
                    .append(val(city.get("timezone"))).append("\t")
                    .append(val(city.get("type"))).append("\t")
                    .append(val(city.get("level")))
                    .append("\n");
        }

        PGConnection pg = connection.unwrap(PGConnection.class);
        CopyManager copy = pg.getCopyAPI();

        try (Reader reader = new StringReader(sb.toString())) {
            copy.copyIn("""
            COPY staging_city_json
            FROM STDIN
            WITH (FORMAT csv, DELIMITER E'\\t', NULL '\\N')
        """, reader);
        }
    }

    private void mergeCitiesFromJson(Connection connection) throws Exception {

        try (Statement stmt = connection.createStatement()) {

            // 1️⃣ INSERT neue Städte
            stmt.execute("""
            INSERT INTO city
                (state_id, name, location, population, timezone_id, type, level)
            SELECT
                st.id,
                s.name,
                CASE
                    WHEN s.longitude IS NOT NULL AND s.latitude IS NOT NULL
                    THEN ST_SetSRID(ST_MakePoint(s.longitude, s.latitude), 4326)::geography
                END,
                s.population,
                tz.id,
                s.type,
                s.level
            FROM staging_city_json s
            JOIN country c
              ON c.iso2 = s.country_code
            JOIN state st
              ON st.country_id = c.id
             AND st.code = s.state_code
            LEFT JOIN timezone tz
              ON tz.zone_name = s.timezone
            ON CONFLICT DO NOTHING
        """);

            // 2️⃣ UPDATE existierende Städte (case-insensitive)
            stmt.execute("""
            UPDATE city ci
            SET
                population  = s.population,
                timezone_id = tz.id,
                type        = s.type,
                level       = s.level
            FROM staging_city_json s
            JOIN country c
              ON c.iso2 = s.country_code
            JOIN state st
              ON st.country_id = c.id
             AND st.code = s.state_code
            LEFT JOIN timezone tz
              ON tz.zone_name = s.timezone
            WHERE ci.state_id = st.id
              AND lower(ci.name) = lower(s.name)
        """);
        }
    }

    private void mergeStatesFromCity(Connection connection) throws Exception {

        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
            INSERT INTO state (country_id, code, name)
            SELECT DISTINCT
                c.id,
                s.admin_code1,
                s.admin_name1
            FROM staging_postal s
            JOIN country c
              ON c.iso2 = s.country_code
            WHERE
                s.admin_name1 IS NOT NULL
                AND s.admin_code1 IS NOT NULL
            ON CONFLICT (country_id, code) DO NOTHING
        """);
        }
    }

    private void mergePostalCodes(Connection connection) throws Exception {

        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
            INSERT INTO postal_code
                (country_id, city_id, code, location, accuracy)
            SELECT
                c.id,
                ci.id,
                s.postal_code,
                CASE
                    WHEN s.longitude IS NOT NULL
                     AND s.latitude IS NOT NULL
                    THEN ST_SetSRID(ST_MakePoint(s.longitude, s.latitude), 4326)::geography
                END,
                s.accuracy
            FROM staging_postal s
            JOIN country c
              ON c.iso2 = s.country_code
            JOIN state st
              ON st.country_id = c.id
             AND st.code = s.admin_code1
            JOIN city ci
              ON ci.state_id = st.id
             AND lower(ci.name) = lower(s.place_name)
            WHERE s.postal_code IS NOT NULL
              AND s.postal_code <> ''
            ON CONFLICT (country_id, city_id, code) DO NOTHING
        """);
        }
    }

    private void importGeoNamesCity(Connection connection, ClassPathResource resource) throws Exception {

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS staging_postal");

            stmt.execute("""
            CREATE TABLE staging_postal (
                country_code VARCHAR(2),
                postal_code VARCHAR(20),
                place_name VARCHAR(180),
                admin_name1 VARCHAR(100),
                admin_code1 VARCHAR(20),
                admin_name2 VARCHAR(100),
                admin_code2 VARCHAR(20),
                admin_name3 VARCHAR(100),
                admin_code3 VARCHAR(20),
                latitude DOUBLE PRECISION,
                longitude DOUBLE PRECISION,
                accuracy INTEGER
            )
        """);
        }

        PGConnection pg = connection.unwrap(PGConnection.class);
        CopyManager copy = pg.getCopyAPI();

        // JAR/Docker safe: read from classpath stream
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            copy.copyIn("""
                COPY staging_postal
                FROM STDIN
                WITH (FORMAT csv, DELIMITER E'\\t')
            """, reader);
        }
    }

    private void dropStagingTables(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS staging_postal");
            stmt.execute("DROP TABLE IF EXISTS staging_city_json");
        }
    }

    private String val(Object o) {
        return o == null ? "\\N" : o.toString();
    }
}
