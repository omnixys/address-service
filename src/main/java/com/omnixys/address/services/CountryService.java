package com.omnixys.address.services;

import com.omnixys.address.models.entity.Country;
import com.omnixys.address.models.inputs.CountryFilterInput;
import com.omnixys.address.repository.CountryRepository;
import com.omnixys.logger.logging.OmnixysLogger;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class CountryService {

    private final CountryRepository countryRepository;
    private final OmnixysLogger logger;

    public List<Country> findAllCountries() {

        log.debug("Fetching all countries");


        var countries = countryRepository.findAll();

        if (countries.isEmpty()) {
            log.debug("No countries found in database");
        } else {
            log.debug("Fetched {} countries", countries.size());
        }

        return countries;
    }

    // =====================================================
    // FIND BY ID
    // =====================================================
    public Country findById(UUID id) {

        log.debug("Fetching country by id={}", id);

        return countryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Country not found for id={}", id);
                    return new IllegalArgumentException("Country not found: " + id);
                });
    }

    public Country findByName(String name) {

        logger.debug("find Country By Name name={}", name);

        return countryRepository.findByName(name)
                .orElseThrow(() -> {
                    log.warn("Country not found for name={}", name);
                    return new IllegalArgumentException("Country not found: " + name);
                });

    }

    // =====================================================
    // FIND BY ISO2
    // =====================================================
    public Country findByIso2(String iso2) {

        String normalized = iso2 == null ? null : iso2.trim().toUpperCase();

        log.debug("Fetching country by iso2={}", normalized);

        return countryRepository.findByIso2IgnoreCase(normalized)
                .orElseThrow(() -> {
                    log.warn("Country not found for iso2={}", normalized);
                    return new IllegalArgumentException("Country not found: " + normalized);
                });
    }

    // =====================================================
    // FIND BY ISO3
    // =====================================================
    public Country findByIso3(String iso3) {

        String normalized = iso3 == null ? null : iso3.trim().toUpperCase();

        log.debug("Fetching country by iso3={}", normalized);

        return countryRepository.findByIso3IgnoreCase(normalized)
                .orElseThrow(() -> {
                    log.warn("Country not found for iso3={}", normalized);
                    return new IllegalArgumentException("Country not found: " + normalized);
                });
    }

    // =====================================================
    // FIND ALL WITH OPTIONAL FILTERS
    // =====================================================
    public Page<Country> findAll(CountryFilterInput filter, Pageable pageable) {

        log.info("Fetching countries with filter={} page={} size={}",
                filter, pageable.getPageNumber(), pageable.getPageSize());

        Specification<Country> spec = buildSpecification(filter);

        Page<Country> result = countryRepository.findAll(spec, pageable);

        log.debug("Country query returned {} results (total={})",
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
    }

    // =====================================================
    // SPECIFICATION BUILDER
    // =====================================================
    private Specification<Country> buildSpecification(CountryFilterInput filter) {

        return (root, query, cb) -> {

            // Apply fetch joins only for select query (not count query)
            if (Country.class.equals(query.getResultType())) {
                root.fetch("continent", JoinType.LEFT);
                root.fetch("subregion", JoinType.LEFT);
                root.fetch("currency", JoinType.LEFT);
                root.fetch("callingCode", JoinType.LEFT);
                root.fetch("languages", JoinType.LEFT);
                root.fetch(  "timezones", JoinType.LEFT);
                query.distinct(true);
            }

            var predicates = cb.conjunction();

            if (filter == null) {
                return predicates;
            }

            // Name
            if (filter.name() != null && !filter.name().isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(cb.lower(root.get("name")),
                                "%" + filter.name().toLowerCase() + "%"));
            }

            // Continent
            if (filter.continent() != null && !filter.continent().isBlank()) {
                var continentJoin = root.join("continent", JoinType.LEFT);

                predicates = cb.and(predicates,
                        cb.equal(cb.lower(continentJoin.get("name")),
                                filter.continent().toLowerCase()));
            }

            // Subregion
            if (filter.subregion() != null && !filter.subregion().isBlank()) {
                var subregionJoin = root.join("subregion", JoinType.LEFT);

                predicates = cb.and(predicates,
                        cb.equal(cb.lower(subregionJoin.get("name")),
                                filter.subregion().toLowerCase()));
            }

            // Currency
            if (filter.currencyCode() != null && !filter.currencyCode().isBlank()) {
                var currencyJoin = root.join("currency", JoinType.LEFT);

                predicates = cb.and(predicates,
                        cb.equal(cb.upper(currencyJoin.get("code")),
                                filter.currencyCode().toUpperCase()));
            }

            // Calling Code
            if (filter.callingCode() != null && !filter.callingCode().isBlank()) {
                var callingJoin = root.join("callingCode", JoinType.LEFT);

                predicates = cb.and(predicates,
                        cb.equal(callingJoin.get("code"),
                                filter.callingCode()));
            }

            return predicates;
        };
    }

}