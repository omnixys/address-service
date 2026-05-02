package com.omnixys.address.models.payload;

import java.util.UUID;

public record GeoLocationInfo(
        double lat,
        double lon,

        UUID countryId,
        UUID stateId,
        UUID cityId,
        UUID postalCodeId,
        UUID streetId,
        UUID houseNumberId,

        String country,
        String state,
        String city,
        String postalCode,
        String street,
        String houseNumber
) {}
