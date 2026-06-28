package com.omnixys.address.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final Flyway flyway;

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean clean() {

        log.warn("⚠️ Flyway CLEAN triggered via /admin/clean");

        flyway.clean();
        flyway.migrate();

        return true;
    }
}
