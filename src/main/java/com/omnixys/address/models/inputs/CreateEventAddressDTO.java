package com.omnixys.address.models.inputs;

import java.util.UUID;

public record CreateEventAddressDTO(
        UUID eventId,
        String street,
        String houseNumber,
        String postalCode,
        String city,
        String state,
        String country,
        String additionalInfo
) {}