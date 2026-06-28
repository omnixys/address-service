package com.omnixys.address.services;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.omnixys.address.models.dto.CountryDTO;
import com.omnixys.address.models.dto.CountryDetailsDTO;
import com.omnixys.address.models.entity.*;
import com.omnixys.address.models.entity.Currency;
import com.omnixys.address.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CountrySeederService {

    private final CountryRepository countryRepository;
    private final ContinentRepository continentRepository;
    private final SubregionRepository subregionRepository;
    private final CurrencyRepository currencyRepository;
    private final LanguageRepository languageRepository;
    private final TimezoneRepository timezoneRepository;
    private final CallingCodeRepository callingCodeRepository;

    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    private final RestClient restClient = RestClient.create();

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    private static final int BATCH_SIZE = 100;

    // =====================================================
    // ENTRY
    // =====================================================

    public void seedCountries() {

        if (!seedEnabled) {
            throw new IllegalStateException("Seeding disabled");
        }

        // 1️⃣ Load base data from API
        List<CountryDTO> apiCountries =
                restClient.get()
                        .uri("https://www.apicountries.com/countries")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

        if (apiCountries == null || apiCountries.isEmpty()) return;

        // 2️⃣ Load detail data from local JSON
        Map<String, CountryDetailsDTO> detailsMap = loadCountryDetails();

        // 3️⃣ Load existing countries
        Map<String, Country> existing =
                countryRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                c -> c.getIso2().toUpperCase(),
                                c -> c
                        ));

        int counter = 0;

        for (CountryDTO api : apiCountries) {

            if (api == null || api.alpha2Code() == null) continue;

            String iso2 = api.alpha2Code().toUpperCase();

            Country country =
                    existing.getOrDefault(iso2, new Country());

            if (country.getId() == null) {
                country.setIso2(iso2);
            }

            CountryDetailsDTO details = detailsMap.get(iso2);

            applyScalar(country, api, details);

            Continent continent = resolveContinent(api);
            country.setContinent(continent);

            Subregion subregion = resolveSubregion(api, continent);
            country.setSubregion(subregion);

            syncCurrency(country, api);
            syncCallingCode(country, api);
            syncLanguages(country, api);
            syncTimezones(country, details);

            em.merge(country);

            if (++counter % BATCH_SIZE == 0) {
                em.flush();
                em.clear();
            }
        }

        em.flush();
        em.clear();
    }

    // =====================================================
    // LOAD JSON DETAILS
    // =====================================================

    private Map<String, CountryDetailsDTO> loadCountryDetails() {

        try {
            InputStream is =
                    new ClassPathResource("data/countries.json")
                            .getInputStream();

            List<CountryDetailsDTO> list =
                    objectMapper.readValue(is, new TypeReference<>() {});

            return list.stream()
                    .filter(d -> d.iso2() != null)
                    .collect(Collectors.toMap(
                            d -> d.iso2().toUpperCase(),
                            d -> d
                    ));

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load countries.json", e);
        }
    }

    // =====================================================
    // SCALAR
    // =====================================================

    private void applyScalar(Country country,
                             CountryDTO api,
                             CountryDetailsDTO details) {

        country.setName(api.name());
        country.setIso3(api.alpha3Code());

        if (api.flags() != null) {
            country.setFlagSvg(api.flags().svg());
            country.setFlagPng(api.flags().png());
        }

        if (details != null) {
            country.setNumericCode(details.numericCode());
            country.setNationality(details.nationality());
            country.setPopulation(details.population());
            country.setAreaSqKm(details.areaSqKm());
            country.setTld(details.tld());

            if (details.latitude() != null) {
                country.setLatitude(new BigDecimal(details.latitude()));
            }

            if (details.longitude() != null) {
                country.setLongitude(new BigDecimal(details.longitude()));
            }
        }
    }

    // =====================================================
    // CONTINENT / SUBREGION
    // =====================================================

    private Continent resolveContinent(CountryDTO api) {

        String name =
                Optional.ofNullable(api.region()).orElse("Unknown");

        return continentRepository.findByName(name)
                .orElseGet(() ->
                        continentRepository.save(
                                Continent.builder()
                                        .name(name)
                                        .build()
                        )
                );
    }

    private Subregion resolveSubregion(CountryDTO api,
                                       Continent continent) {

        String name =
                Optional.ofNullable(api.subregion())
                        .orElse("Unknown");

        return subregionRepository
                .findByNameAndContinent(name, continent)
                .orElseGet(() ->
                        subregionRepository.save(
                                Subregion.builder()
                                        .name(name)
                                        .continent(continent)
                                        .build()
                        )
                );
    }

    // =====================================================
    // RELATIONS
    // =====================================================

    private void syncCurrency(Country country, CountryDTO api) {

        if (api.currencies() == null
                || api.currencies().isEmpty()) return;

        CountryDTO.CurrencyDTO c =
                api.currencies().get(0);

        String code = trimUpper(c.code());
        if (code == null) return;

        Currency currency =
                currencyRepository.findByCode(code)
                        .orElseGet(() ->
                                currencyRepository.save(
                                        Currency.builder()
                                                .code(code)
                                                .name(nullToEmpty(c.name()))
                                                .symbol(nullToEmpty(c.symbol()))
                                                .build()
                                )
                        );

        country.setCurrency(currency);
    }

    private void syncCallingCode(Country country, CountryDTO api) {

        if (api.callingCodes() == null
                || api.callingCodes().isEmpty()) return;

        String raw = api.callingCodes().get(0);
        if (raw == null) return;

        String formatted =
                raw.startsWith("+") ? raw : "+" + raw.trim();

        CallingCode callingCode =
                callingCodeRepository.findByCode(formatted)
                        .orElseGet(() ->
                                callingCodeRepository.save(
                                        CallingCode.builder()
                                                .code(formatted)
                                                .build()
                                )
                        );

        country.setCallingCode(callingCode);
    }

    private void syncLanguages(Country country, CountryDTO api) {

        if (api.languages() == null) return;

        Set<Language> desired = new HashSet<>();

        for (CountryDTO.LanguageDTO l : api.languages()) {

            if (l == null) continue;

            String iso2 = trimUpper(l.iso639_1());
            if (iso2 == null) continue;

            Language language =
                    languageRepository.findByIso2(iso2)
                            .orElseGet(() ->
                                    languageRepository.save(
                                            Language.builder()
                                                    .iso2(iso2)
                                                    .iso3(trimUpper(l.iso639_2()))
                                                    .name(l.name())
                                                    .build()
                                    )
                            );

            desired.add(language);
        }

        country.getLanguages().clear();
        country.getLanguages().addAll(desired);
    }

    private void syncTimezones(Country country,
                               CountryDetailsDTO details) {

        if (details == null
                || details.timezones() == null) return;

        Set<Timezone> desired = new HashSet<>();

        for (CountryDetailsDTO.TimezoneDTO tz :
                details.timezones()) {

            Timezone timezone =
                    timezoneRepository
                            .findByZoneName(tz.zoneName())
                            .orElseGet(() ->
                                    timezoneRepository.save(
                                            Timezone.builder()
                                                    .zoneName(tz.zoneName())
                                                    .gmtOffset(tz.gmtOffset())
                                                    .gmtOffsetName(tz.gmtOffsetName())
                                                    .abbreviation(tz.abbreviation())
                                                    .tzName(tz.tzName())
                                                    .build()
                                    )
                            );

            desired.add(timezone);
        }

        country.getTimezones().clear();
        country.getTimezones().addAll(desired);
    }

    // =====================================================
    // UTIL
    // =====================================================

    private static String trimUpper(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toUpperCase();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}