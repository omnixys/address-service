package com.omnixys.address.services;

import com.omnixys.address.models.dto.GeoapifyAutocompleteResponse;
import com.omnixys.address.models.entity.City;
import com.omnixys.address.models.entity.HouseNumber;
import com.omnixys.address.models.entity.PostalCode;
import com.omnixys.address.models.entity.Street;
import com.omnixys.address.models.payload.AddressAutocompletePayload;
import com.omnixys.address.models.payload.GeoLocationInfo;
import com.omnixys.address.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoapifyAutocompleteService {

    private final RestClient restClient = RestClient.create();

    private final PostalCodeService postalCodeService;
    private final CityService cityService;
    private final StreetService streetService;
    private final HouseNumberService houseNumberService;
    private final CountryService countryService;
    private final StateService stateService;

    private final PostalCodeRepository postalCodeRepository;
    private final CityRepository cityRepository;
    private final StreetRepository streetRepository;
    private final HouseNumberRepository houseNumberRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;

    @Value("${app.geoapify.apiKey}")
    private String apiKey;

    private static final GeometryFactory GEO_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public List<AddressAutocompletePayload> autocomplete(String text, String countryCode, Integer limit) {

        log.debug("Geoapify autocomplete called: text='{}', countryCode='{}'", text, countryCode);

       // String safeText = text.replace(" ", "+");
        String saferText = text == null ? "" : text.trim();
        var encodedText = URLEncoder.encode(saferText, StandardCharsets.UTF_8);

        GeoapifyAutocompleteResponse response = restClient.get()
                .uri(builder -> {
                    var uriBuilder = builder
                            .scheme("https")
                            .host("api.geoapify.com")
                            .path("/v1/geocode/autocomplete")
                            .queryParam("text", text)
                            .queryParam("limit", limit != null ? limit : 5)
                           // .queryParam("type", "street")
                            .queryParam("format", "json")
                            .queryParam("apiKey", apiKey);

                    if (countryCode != null && !countryCode.isBlank()) {
                        uriBuilder.queryParam("filter", "countrycode:" + countryCode.toLowerCase(Locale.ROOT));
                    }


                    var uri = uriBuilder.build();
                    log.debug("Geoapify request URI: {}", uri);
                    return uri;
                })
                .retrieve()
                .body(GeoapifyAutocompleteResponse.class);

        if (response == null || response.results() == null || response.results().isEmpty()) {
            log.warn("Autocomplete returned no results.");
            return List.of();
        }

        List<GeoapifyAutocompleteResponse.Result> validResults =
                response.results().stream()
                        .filter(r -> r.rank() != null)
                        .filter(r -> r.rank().confidence() != null)
                        .filter(r -> r.rank().confidence() == 1)
                        .toList();

        List<AddressAutocompletePayload> payloads =
                validResults.stream()
                        .map(r -> new AddressAutocompletePayload(
                                r.formatted(),
                                r.street(),
                                r.housenumber(),
                                r.postcode(),
                                r.city(),
                                r.state(),
                                r.country(),
                                r.rank().confidence(),
                                r.lat(),
                                r.lon()
                        ))
                        .collect(Collectors.toList());

        // Persist streets + house numbers (best-effort, but inside transaction)
        for (var p : payloads) {
            if (isBlank(p.street()) || isBlank(p.city())) {
                continue;
            }

            Optional<City> cityOpt = resolveCity(p, countryCode);
            if (cityOpt.isEmpty()) {
                continue;
            }

            City city = cityOpt.get();
            Point point = toPoint(p.lon(), p.lat());

            Street street = findOrCreateStreet(city.getId(), city, p.street(), point);

            if (!isBlank(p.houseNumber())) {
                findOrCreateHouseNumber(street.getId(), street, p.houseNumber(), point);
            }
        }

        log.debug("Autocomplete returned {} results.", payloads.size());
        return payloads;
    }

    @Transactional
    public GeoLocationInfo getGeoLocationInfo(
            final String text,
            final String countryCode,
            final Integer limit
    ) {
        log.debug("GraphQL getGeoLocationInfo called: text='{}'", text);

        var suggestion = autocomplete(text, countryCode, limit)
                .stream()
                .findFirst()
                .orElse(null);

        if (suggestion == null) {
            log.debug("No address found for text='{}'", text);
            return null;
        }

        /**
         * -------------------------------------------------------------
         * STEP 1: Normalize incoming data
         * -------------------------------------------------------------
         */
        String countryName = suggestion.country();
        String stateName = suggestion.state();
        String cityName = suggestion.city();
        String postalCode = suggestion.postalCode();
        String streetName = suggestion.street();
        String houseNumber = suggestion.houseNumber();

        /**
         * -------------------------------------------------------------
         * STEP 2: Resolve DB entities (IMPORTANT)
         * -------------------------------------------------------------
         */
        var country = countryService.findByName(countryName);

        var state = stateService.findByNameAndCountryId(
                stateName,
                country.getId()

        );

        var city = cityService.findByNameAndStateId(
                cityName,
                state.getId()
        );

        var postal = postalCodeService.findByCodeAndCityId(
                postalCode,
                city.getId()
        );

        var street = streetService.findByNameAndCityId(
                streetName,
                city.getId()
        );

        var house = houseNumberService.findByHouseNumberAndStreetId(
                houseNumber,
                street.getId()
        );

        /**
         * -------------------------------------------------------------
         * STEP 3: RETURN FULL STRUCTURE
         * -------------------------------------------------------------
         */
        return new GeoLocationInfo(
                suggestion.lat(),
                suggestion.lon(),

                country.getId(),
                state != null ? state.getId() : null,
                city.getId(),
                postal != null ? postal.getId() : null,
                street.getId(),
                house.getId(),

                country.getName(),
                state.getName(),
                city.getName(),
                postal != null ? postal.getCode(): null,
                street.getName(),
                house.getNumber()
        );
    }

    /**
     * 1) If postcode exists -> PostalCode(countryIso2, zip) -> city
     * 2) else fallback -> City(name + state.name + countryIso2)
     */
    private Optional<City> resolveCity(AddressAutocompletePayload p, String countryCode) {

        String code = countryCode;

        if (code == null || code.isBlank()) {
            var country = countryRepository.findByName(p.country()).orElseThrow(() -> {
                log.warn("Country not found for name={}", p.country());
                return new IllegalArgumentException("Country not found: " + p.country());
            });
            code = country.getIso2();
        }

        String iso2 = normalizeIso2(code, p.country());
        if (iso2 == null) return Optional.empty();

        // (1) Strong resolver: zip + country iso2, but may return multiple rows
        if (!isBlank(p.postalCode())) {
            List<PostalCode> candidates = postalCodeRepository.findAllByCountryIso2AndCode(iso2, p.postalCode().trim());

            if (!candidates.isEmpty()) {
                // 1) Prefer exact city name match (case-insensitive)
                if (!isBlank(p.city())) {
                    String cityName = p.city().trim();
                    for (PostalCode pc : candidates) {
                        City c = pc.getCity();
                        if (c != null && c.getName() != null && c.getName().equalsIgnoreCase(cityName)) {
                            return Optional.of(c);
                        }
                    }
                }

                // 2) Prefer nearest to provided coordinates (if available)
                if (p.lat() != null && p.lon() != null) {
                    City best = null;
                    double bestScore = Double.POSITIVE_INFINITY;

                    for (PostalCode pc : candidates) {
                        // Prefer PostalCode.location if present, else City.location
                        Point loc = pc.getLocation() != null ? pc.getLocation() :
                                (pc.getCity() != null ? pc.getCity().getLocation() : null);

                        if (loc == null) continue;

                        double d = haversineMeters(p.lat(), p.lon(), loc.getY(), loc.getX());
                        if (d < bestScore) {
                            bestScore = d;
                            best = pc.getCity();
                        }
                    }

                    if (best != null) return Optional.of(best);
                }

                // 3) Fallback: deterministic first
                return Optional.ofNullable(candidates.get(0).getCity());
            }
        }

        // (2) Fallback: city + state + country iso2
        if (!isBlank(p.state()) && !isBlank(p.city())) {
            return cityRepository.findByCityStateCountry(p.city().trim(), p.state().trim(), iso2);
        }

        return Optional.empty();
    }

    private Street findOrCreateStreet(UUID cityId, City city, String streetName, Point location) {
        Optional<Street> existing = streetRepository.findByCityIdAndNameIgnoreCase(cityId, streetName);
        if (existing.isPresent()) {
            return existing.get();
        }

        Street created = Street.builder()
                .city(city)
                .name(streetName.trim())
                .location(location)
                .build();

        try {
            return streetRepository.save(created);
        } catch (DataIntegrityViolationException dup) {
            return streetRepository.findByCityIdAndNameIgnoreCase(cityId, streetName)
                    .orElseThrow(() -> dup);
        }
    }

    private HouseNumber findOrCreateHouseNumber(UUID streetId, Street street, String number, Point location) {
        Optional<HouseNumber> existing = houseNumberRepository.findByStreetIdAndNumberIgnoreCase(streetId, number);
        if (existing.isPresent()) {
            return existing.get();
        }

        HouseNumber created = HouseNumber.builder()
                .street(street)
                .number(number.trim())
                .location(location)
                .build();

        try {
            return houseNumberRepository.save(created);
        } catch (DataIntegrityViolationException dup) {
            return houseNumberRepository.findByStreetIdAndNumberIgnoreCase(streetId, number)
                    .orElseThrow(() -> dup);
        }
    }

    private static Point toPoint(Double lon, Double lat) {
        if (lon == null || lat == null) return null;
        return GEO_FACTORY.createPoint(new Coordinate(lon, lat));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String normalizeIso2(String countryCodeParam, String countryNameFromApi) {
        // Prefer explicit filter country code.
        if (countryCodeParam != null && !countryCodeParam.isBlank()) {
            return countryCodeParam.trim().toUpperCase(Locale.ROOT);
        }
        // If you want to map country names -> iso2, do it via your Country table (recommended),
        // but that requires CountryRepository. For now, we cannot reliably infer iso2 from a name.
        return null;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}