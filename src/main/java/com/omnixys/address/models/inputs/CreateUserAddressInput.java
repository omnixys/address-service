package com.omnixys.address.models.inputs;

import com.omnixys.address.models.enums.AddressType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserAddressInput(
        @NotNull UUID userId,
        @NotNull AddressType addressType,
        @NotNull UUID streetId,
        @NotNull UUID postalCodeId,
        @NotNull UUID cityId,
        @NotNull UUID stateId,
        @NotNull UUID countryId,
        UUID houseNumberId,
        String additionalInfo
) {}