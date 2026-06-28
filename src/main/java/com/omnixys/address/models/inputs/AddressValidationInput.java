package com.omnixys.address.models.inputs;

import jakarta.validation.constraints.NotBlank;

public record AddressValidationInput(
        @NotBlank String street,
        String houseNumber,
        @NotBlank String postalCode,
        @NotBlank String city,
        String state,
        @NotBlank String country
) {}