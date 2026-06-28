package com.omnixys.address.models.inputs;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateEventAddressInput(
        @NotNull UUID id,
        @NotNull UUID eventId,
        String street,
        String houseNumber,
        UUID postalCodeId,
        UUID cityId,
        UUID stateId,
        UUID countryId,
        String additionalInfo
) {
}
