package com.omnixys.address.services;

import com.omnixys.address.models.entity.UserAddress;
import com.omnixys.address.models.enums.AddressType;
import com.omnixys.address.models.inputs.CreateUserAddressInput;
import com.omnixys.address.repository.UserAddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository repository;

    @Mock
    private CountryService countryService;

    @Mock
    private StateService stateService;

    @Mock
    private CityService cityService;

    @Mock
    private PostalCodeService postalCodeService;

    @Mock
    private StreetService streetService;

    @Mock
    private HouseNumberService houseNumberService;

    @Mock
    private AddressCacheService addressCacheService;

    @InjectMocks
    private UserAddressService service;

    @Captor
    private ArgumentCaptor<UserAddress> addressCaptor;

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

        var saved = UserAddress.builder()
                .id(addressId)
                .userId(userId)
                .countryId(countryId)
                .stateId(stateId)
                .cityId(cityId)
                .postalCodeId(postalCodeId)
                .streetId(streetId)
                .addressType(AddressType.HOME)
                .build();

        when(repository.save(any(UserAddress.class))).thenReturn(saved);

        var result = service.createUserAddress(input);

        assertNotNull(result);
        assertEquals(addressId, result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(AddressType.HOME, result.getAddressType());
        assertEquals(countryId, result.getCountryId());
        assertEquals(stateId, result.getStateId());
        assertEquals(cityId, result.getCityId());
        assertEquals(postalCodeId, result.getPostalCodeId());
        assertEquals(streetId, result.getStreetId());

        verify(repository).save(addressCaptor.capture());
        var captured = addressCaptor.getValue();
        assertEquals(userId, captured.getUserId());
        assertEquals(AddressType.HOME, captured.getAddressType());
    }
}
