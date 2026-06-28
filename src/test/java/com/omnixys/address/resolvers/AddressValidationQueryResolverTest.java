package com.omnixys.address.resolvers;

import com.omnixys.address.models.inputs.AddressValidationInput;
import com.omnixys.address.models.payload.AddressValidationPayload;
import com.omnixys.address.services.GeoapifyAddressValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressValidationQueryResolverTest {

    @Mock
    private GeoapifyAddressValidationService validationService;

    @InjectMocks
    private AddressValidationQueryResolver resolver;

    @Test
    void validateAddressReturnsValid() {
        var input = new AddressValidationInput(
                "Main St", "123", "62701", "Springfield", "IL", "US"
        );

        var payload = new AddressValidationPayload(
                true, "OK", 1.0, "123 Main St, Springfield, IL 62701", -89.0, 39.0
        );

        when(validationService.validateStrict(any(AddressValidationInput.class)))
                .thenReturn(payload);

        var result = resolver.validateAddress(input);

        assertNotNull(result);
        assertTrue(result.valid());
        assertEquals("OK", result.reason());
        assertEquals(1.0, result.confidence());
        assertEquals("123 Main St, Springfield, IL 62701", result.formatted());
    }

    @Test
    void validateAddressReturnsInvalid() {
        var input = new AddressValidationInput(
                "Nowhere Ln", "999", "00000", "Ghost Town", "ZZ", "XX"
        );

        var payload = new AddressValidationPayload(
                false, "NO_RESULTS", null, null, null, null
        );

        when(validationService.validateStrict(any(AddressValidationInput.class)))
                .thenReturn(payload);

        var result = resolver.validateAddress(input);

        assertNotNull(result);
        assertFalse(result.valid());
        assertEquals("NO_RESULTS", result.reason());
        assertNull(result.confidence());
    }
}
