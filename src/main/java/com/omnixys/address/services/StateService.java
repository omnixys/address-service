package com.omnixys.address.services;

import com.omnixys.address.models.entity.Country;
import com.omnixys.address.models.entity.State;
import com.omnixys.address.models.inputs.StateFilterInput;
import com.omnixys.address.repository.CountryRepository;
import com.omnixys.address.repository.StateRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static org.springframework.util.StringUtils.hasText;

@RequiredArgsConstructor
@Service
@Slf4j
public class StateService {
    public final StateRepository stateRepository;
    private final CountryRepository countryRepository;

    public State findByName(final String name) {
        log.debug("Fetching state by name={}", name);

        return stateRepository.findByName(name)
                .orElseThrow(() -> {
                    log.warn("State not found for name={}", name);
                    return new IllegalArgumentException("State not found: " + name);
                });
    }

    public State findByNameAndCountryId(final String name, final UUID countryId) {
        log.debug("Fetching state by name={} and countryID={}", name, countryId);

        return stateRepository.findByNameAndCountryId(name, countryId)
                .orElseThrow(() -> {
                    log.warn("State not found for name={} and countryId={}", name, countryId);
                    return new IllegalArgumentException("State not found: " + name);
                });
    }


    public State findById(UUID id) {
        log.debug("Fetching state by id={}", id);

        return stateRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("State not found for id={}", id);
                    return new IllegalArgumentException("State not found: " + id);
                });
    }

    // =====================================================
    // FIND BY CODE (ISO3166-2)
    // =====================================================

    public State findByCode(String iso3166_2) {
        String normalized = normalizeKeepCase(iso3166_2);
        log.debug("Fetching state by iso3166_2={}", normalized);

        return stateRepository.findByIso3166CodeIgnoreCase(normalized)
                .orElseThrow(() -> {
                    log.warn("State not found for iso3166_2={}", normalized);
                    return new IllegalArgumentException("State not found: " + normalized);
                });
    }

    public List<State> findByCountryId(UUID countryId) {

        log.debug("Fetching states by countryId={}", countryId);

        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> {
                    log.warn("Country not found for id={}", countryId);
                    return new IllegalArgumentException("Country not found: " + countryId);
                });

        List<State> states = stateRepository.findByCountry(country);

        log.debug("Fetched {} states for countryId={}", states.size(), countryId);

        return states;
    }

    public Page<State> findByCountryId(UUID countryId, Pageable pageable) {

        log.debug("Fetching paged states by countryId={} page={} size={}",
                countryId,
                pageable.getPageNumber(),
                pageable.getPageSize());

        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> {
                    log.warn("Country not found for id={}", countryId);
                    return new IllegalArgumentException("Country not found: " + countryId);
                });

        Page<State> page = stateRepository.findByCountry(country, pageable);

        log.debug("Fetched {} states (total={}) for countryId={}",
                page.getNumberOfElements(),
                page.getTotalElements(),
                countryId);

        return page;
    }

// =====================================================
// FIND BY COUNTRY ISO2 (PAGED)
// =====================================================
    public Page<State> findByCountryIso2(String iso2, Pageable pageable) {

        String normalized = normalizeIso(iso2);

        log.debug("Fetching paged states by countryIso2={} page={} size={}",
                normalized,
                pageable.getPageNumber(),
                pageable.getPageSize());

        Country country = countryRepository.findByIso2IgnoreCase(normalized)
                .orElseThrow(() -> {
                    log.warn("Country not found for iso2={}", normalized);
                    return new IllegalArgumentException("Country not found: " + normalized);
                });

        return stateRepository.findByCountry(country, pageable);
    }


    // =====================================================
// FIND BY COUNTRY ISO3 (PAGED)
// =====================================================
    public Page<State> findByCountryIso3(String iso3, Pageable pageable) {

        String normalized = normalizeIso(iso3);

        log.debug("Fetching paged states by countryIso3={} page={} size={}",
                normalized,
                pageable.getPageNumber(),
                pageable.getPageSize());

        Country country = countryRepository.findByIso3IgnoreCase(normalized)
                .orElseThrow(() -> {
                    log.warn("Country not found for iso3={}", normalized);
                    return new IllegalArgumentException("Country not found: " + normalized);
                });

        return stateRepository.findByCountry(country, pageable);
    }

    // =====================================================
    // FIND ALL WITH OPTIONAL FILTERS
    // =====================================================
    public Page<State> findAll(StateFilterInput filter, Pageable pageable) {

        log.info("Fetching states with filter={} page={} size={}",
                filter, pageable.getPageNumber(), pageable.getPageSize());

        Specification<State> spec = buildSpecification(filter);

        Page<State> result = stateRepository.findAll(spec, pageable);

        log.debug("State query returned {} results (total={})",
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
    }

    // =====================================================
    // UTIL
    // =====================================================
    private String normalizeIso(String iso) {
        if (iso == null) return null;
        String t = iso.trim().toUpperCase();
        return t.isEmpty() ? null : t;
    }

    // =====================================================
    // SPECIFICATION BUILDER
    // =====================================================
    // =====================================================
// SPECIFICATION BUILDER
// =====================================================
    private Specification<State> buildSpecification(StateFilterInput filter) {

        return (root, query, cb) -> {

            // Fetch joins only for SELECT query (not count query)
            if (State.class.equals(query.getResultType())) {

                root.fetch("country", JoinType.LEFT);
                root.fetch("parent", JoinType.LEFT);
                root.fetch("timezones", JoinType.LEFT);

                query.distinct(true);
            }

            var predicates = cb.conjunction();

            if (filter == null) {
                return predicates;
            }

            // ============================================
            // NAME
            // ============================================

            if (hasText(filter.name())) {
                predicates = cb.and(predicates,
                        cb.like(cb.lower(root.get("name")),
                                "%" + filter.name().toLowerCase() + "%"));
            }

            // ============================================
            // ISO3166-2
            // ============================================

            if (hasText(filter.iso3166_2())) {
                predicates = cb.and(predicates,
                        cb.equal(cb.upper(root.get("iso3166_2")),
                                filter.iso3166_2().toUpperCase()));
            }

            // ============================================
            // CODE (BDS, BW, CA etc.)
            // ============================================

            if (hasText(filter.code())) {
                predicates = cb.and(predicates,
                        cb.equal(cb.upper(root.get("code")),
                                filter.code().toUpperCase()));
            }

            // ============================================
            // TYPE (province, state, region)
            // ============================================

            if (hasText(filter.type())) {
                predicates = cb.and(predicates,
                        cb.equal(cb.lower(root.get("type")),
                                filter.type().toLowerCase()));
            }

            // ============================================
            // COUNTRY FILTERS
            // ============================================

            if (filter.countryId() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("country").get("id"),
                                filter.countryId()));
            }

            if (hasText(filter.countryIso2())) {
                var countryJoin = root.join("country", JoinType.LEFT);

                predicates = cb.and(predicates,
                        cb.equal(cb.upper(countryJoin.get("iso2")),
                                filter.countryIso2().toUpperCase()));
            }

            if (hasText(filter.countryIso3())) {
                var countryJoin = root.join("country", JoinType.LEFT);

                predicates = cb.and(predicates,
                        cb.equal(cb.upper(countryJoin.get("iso3")),
                                filter.countryIso3().toUpperCase()));
            }

            return predicates;
        };
    }

    private String normalizeKeepCase(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}

