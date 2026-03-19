package com.omnixys.address.models.inputs;

import com.omnixys.address.models.enums.AddressType;

import java.util.UUID;

public record CreateEventAddressInput(
        UUID eventId,
        String street,
        String houseNumber,
        String postalCode,
        String city,
        String state,
        String country,
        String additionalInfo
) {}