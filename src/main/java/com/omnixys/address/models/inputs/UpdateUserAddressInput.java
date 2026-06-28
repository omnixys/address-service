package com.omnixys.address.models.inputs;

import com.omnixys.address.models.enums.AddressType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateUserAddressInput(
        @NotNull UUID id,
        AddressType addressType,
        String additionalInfo
) {
}
