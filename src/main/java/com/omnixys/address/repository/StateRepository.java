package com.omnixys.address.repository;

import com.omnixys.address.models.entity.Country;
import com.omnixys.address.models.entity.State;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StateRepository extends JpaRepository<State, UUID>, JpaSpecificationExecutor<State> {

    @NonNull
    Optional<State> findByName(final String name);

    Optional<State> findByCountryAndCode(Country country, String code);

    @EntityGraph(attributePaths = {
            "parent",
            "timezones"
    })
    List<State> findByCountry(Country country);

    @EntityGraph(attributePaths = {
            "parent",
            "timezones"
    })
    Page<State> findByCountry(Country country, Pageable pageable);

    Optional<State> findByIso3166CodeIgnoreCase(String iso3166Code);

    Optional<State> findByNameAndCountryId(String name, UUID countryId);
}