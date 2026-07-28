package com.omnixys.address.controller;

import com.omnixys.address.services.CountrySeederService;
import com.omnixys.address.services.GlobalPostalImportService;
import com.omnixys.address.services.StateSeederService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/admin/seed")
@RequiredArgsConstructor
public class SeedController {

    private final CountrySeederService countrySeederService;
    private final StateSeederService stateSeederService;
    private final GlobalPostalImportService globalPostalImportService;

    @PostMapping("/countries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> seedCountries() {
        log.info("Seed endpoint called: countries");
        countrySeederService.seedCountries();
        log.info("Country seed completed successfully");
        return ResponseEntity.ok("Countries seeded successfully");
    }

    @PostMapping("/states")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> seedStates() {
        log.info("Seed endpoint called: states");
        stateSeederService.seedStates();
        log.info("State seed completed successfully");
        return ResponseEntity.ok("States seeded successfully");
    }

    @PostMapping("/postal-code")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importPostal() throws Exception {
        log.info("Seed endpoint called: postal-code import");
        globalPostalImportService.importAll();
        log.info("Postal code import completed successfully");
        return ResponseEntity.ok("Postal import finished");
    }

}

