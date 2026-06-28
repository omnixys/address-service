package com.omnixys.address.services;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.omnixys.address.models.dto.StateDTO;
import com.omnixys.address.models.entity.*;
import com.omnixys.address.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StateSeederService {

    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final TimezoneRepository timezoneRepository;

    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    private static final int BATCH_SIZE = 500;

    // =====================================================
    // ENTRY
    // =====================================================

    public void seedStates() {

        if (!seedEnabled) {
            throw new IllegalStateException("State seeding disabled");
        }

        log.info("Starting State seeding with 2-phase parent resolution");

        List<StateDTO> states = loadStatesFromJson();
        if (states.isEmpty()) {
            log.warn("No states found in JSON");
            return;
        }

        Map<String, Country> countries =
                countryRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                c -> c.getIso2().toUpperCase(),
                                c -> c
                        ));

        // 🔥 JSON-ID → ENTITY Mapping
        Map<Long, State> jsonIdToEntity = new HashMap<>();

        int counter = 0;

        // =====================================================
        // PHASE 1 — CREATE / UPDATE STATES (NO PARENTS)
        // =====================================================

        for (StateDTO dto : states) {

            if (dto == null || dto.country_code() == null) continue;

            Country country =
                    countries.get(dto.country_code().toUpperCase());

            if (country == null) {
                log.warn("Country not found for state {}", dto.name());
                continue;
            }

            String iso = normalize(dto.iso3166_2());

            if (iso == null || !iso.contains("-")) {
                continue; // Skip numeric / invalid entries
            }

            String code = iso.substring(iso.indexOf("-") + 1);

            State state = stateRepository
                    .findByCountryAndCode(country, code)
                    .orElseGet(() -> State.builder()
                            .country(country)
                            .code(code)
                            .build());

            applyScalar(state, dto);

            state = em.merge(state);

            jsonIdToEntity.put(dto.id(), state);

            syncTimezone(state, dto);

            if (++counter % BATCH_SIZE == 0) {
                em.flush();
                em.clear();
                log.debug("Phase 1 flush at {}", counter);
            }
        }

        em.flush();
        em.clear();

        log.info("Phase 1 completed. Resolving parent hierarchy...");

        // =====================================================
        // PHASE 2 — SET PARENTS
        // =====================================================

        int parentCounter = 0;

        for (StateDTO dto : states) {

            if (dto.parent_id() == null) continue;

            State child = jsonIdToEntity.get(dto.id());
            State parent = jsonIdToEntity.get(dto.parent_id());

            if (child == null || parent == null) continue;

            child.setParent(parent);

            if (++parentCounter % BATCH_SIZE == 0) {
                em.flush();
                em.clear();
                log.debug("Phase 2 flush at {}", parentCounter);
            }
        }

        em.flush();
        em.clear();

        log.info("State seeding finished. Total states processed: {}", counter);
    }

    // =====================================================
    // LOAD JSON
    // =====================================================

    private List<StateDTO> loadStatesFromJson() {

        try {
            InputStream is =
                    new ClassPathResource("data/states.json")
                            .getInputStream();

            return objectMapper.readValue(is, new TypeReference<>() {});

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load states.json", e);
        }
    }

    // =====================================================
    // APPLY SCALAR
    // =====================================================

    private void applyScalar(State state, StateDTO dto) {

        state.setName(dto.name());
        state.setIso3166Code(dto.iso3166_2());
        state.setType(dto.type());
        state.setLevel(dto.level());
        state.setPopulation(dto.population());

        if (dto.latitude() != null) {
            state.setLatitude(new BigDecimal(dto.latitude()));
        }

        if (dto.longitude() != null) {
            state.setLongitude(new BigDecimal(dto.longitude()));
        }
    }

    // =====================================================
    // TIMEZONE
    // =====================================================

    private void syncTimezone(State state, StateDTO dto) {

        if (dto.timezone() == null) return;

        Timezone timezone =
                timezoneRepository.findByZoneName(dto.timezone())
                        .orElseGet(() ->
                                timezoneRepository.save(
                                        Timezone.builder()
                                                .zoneName(dto.timezone())
                                                .build()
                                )
                        );

        state.getTimezones().clear();
        state.getTimezones().add(timezone);
    }

    // =====================================================
    // UTIL
    // =====================================================

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toUpperCase();
    }
}