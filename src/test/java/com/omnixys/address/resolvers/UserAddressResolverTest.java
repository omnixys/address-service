package com.omnixys.address.resolvers;

import com.omnixys.address.models.entity.UserAddress;
import com.omnixys.address.models.enums.AddressType;
import com.omnixys.address.models.inputs.CreateUserAddressInput;
import com.omnixys.address.services.UserAddressService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAddressResolverTest {

    @Mock
    private UserAddressService userAddressService;

    @InjectMocks
    private UserAddressResolver resolver;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createUserAddressWithValidInput() {
        var userId = UUID.randomUUID();
        var streetId = UUID.randomUUID();
        var postalCodeId = UUID.randomUUID();
        var cityId = UUID.randomUUID();
        var stateId = UUID.randomUUID();
        var countryId = UUID.randomUUID();
        var addressId = UUID.randomUUID();

        var input = new CreateUserAddressInput(
                userId,
                AddressType.HOME,
                streetId,
                postalCodeId,
                cityId,
                stateId,
                countryId,
                null,
                null
        );

        var address = UserAddress.builder()
                .id(addressId)
                .userId(userId)
                .addressType(AddressType.HOME)
                .build();

        when(userAddressService.createUserAddress(any(CreateUserAddressInput.class)))
                .thenReturn(address);

        var result = resolver.createUserAddress(input);

        assertNotNull(result);
        assertEquals(addressId, result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(AddressType.HOME, result.getAddressType());
    }

    @Test
    void createUserAddressWithNullUserIdFailsValidation() {
        var input = new CreateUserAddressInput(
                null,
                AddressType.HOME,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null
        );

        var violations = validator.validate(input);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("userId")));
    }

    @Test
    void createUserAddressWithNullRequiredFieldsFailsValidation() {
        var input = new CreateUserAddressInput(
                UUID.randomUUID(),
                AddressType.HOME,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var violations = validator.validate(input);
        assertFalse(violations.isEmpty());
        var propertyPaths = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();
        assertTrue(propertyPaths.containsAll(
                java.util.List.of("streetId", "postalCodeId", "cityId", "stateId", "countryId")));
    }
}
