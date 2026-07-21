package com.omnixys.address.controller;

import com.omnixys.address.services.CountrySeederService;
import com.omnixys.address.services.GlobalPostalImportService;
import com.omnixys.address.services.StateSeederService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SeedGraphqlController {

    private final CountrySeederService countrySeederService;
    private final StateSeederService stateSeederService;
    private final GlobalPostalImportService globalPostalImportService;

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> seedCountries() {
        try {
            countrySeederService.seedCountries();
            return Map.of("message", "Countries seeded successfully", "success", true);
        } catch (Exception e) {
            log.error("Failed to seed countries", e);
            return Map.of("message", "Failed to seed countries: " + e.getMessage(), "success", false);
        }
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> seedStates() {
        try {
            stateSeederService.seedStates();
            return Map.of("message", "States seeded successfully", "success", true);
        } catch (Exception e) {
            log.error("Failed to seed states", e);
            return Map.of("message", "Failed to seed states: " + e.getMessage(), "success", false);
        }
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> seedPostalCodes() {
        try {
            globalPostalImportService.importAll();
            return Map.of("message", "Postal import finished", "success", true);
        } catch (Exception e) {
            log.error("Failed to import postal codes", e);
            return Map.of("message", "Failed to import postal codes: " + e.getMessage(), "success", false);
        }
    }
}
