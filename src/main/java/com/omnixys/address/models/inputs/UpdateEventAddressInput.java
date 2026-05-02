package com.omnixys.address.models.inputs;

import java.util.UUID;

public record UpdateEventAddressInput(
        UUID id,
        UUID eventId,
        String street,
        String houseNumber,
        UUID postalCodeId,
        UUID cityId,
        UUID stateId,
        UUID countryId,
        String additionalInfo
) {
}
