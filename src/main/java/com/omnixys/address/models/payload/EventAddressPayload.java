package com.omnixys.address.models.payload;

import com.omnixys.address.models.entity.EventAddress;
import com.omnixys.address.models.enums.AddressType;

import java.util.UUID;

public record EventAddressPayload(
        UUID id,
        UUID eventId,
        String country,
        String state,
        String city,
        String postalCode,
        String street,
        String houseNumber,
        String additionalInfo,
        Double lat,
        Double lon
) {
}
