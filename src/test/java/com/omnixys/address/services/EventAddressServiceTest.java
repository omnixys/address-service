package com.omnixys.address.services;

import com.omnixys.address.exception.AddressNotFoundException;
import com.omnixys.address.repository.EventAddressProjection;
import com.omnixys.address.repository.EventAddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventAddressServiceTest {

    @Mock
    private EventAddressRepository repository;
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
    private EventAddressProjection projection;

    @InjectMocks
    private EventAddressService service;

    @Test
    void findByEventIdMapsTypedProjection() {
        var id = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        when(repository.findProjectedByEventId(eventId)).thenReturn(Optional.of(projection));
        when(projection.getId()).thenReturn(id);
        when(projection.getEventId()).thenReturn(eventId);
        when(projection.getCountry()).thenReturn("Germany");
        when(projection.getCity()).thenReturn("Stuttgart");
        when(projection.getLat()).thenReturn(48.7758);
        when(projection.getLon()).thenReturn(9.1829);

        var result = service.findByEventId(eventId);

        assertEquals(id, result.id());
        assertEquals(eventId, result.eventId());
        assertEquals("Germany", result.country());
        assertEquals("Stuttgart", result.city());
        assertEquals(48.7758, result.lat());
        assertEquals(9.1829, result.lon());
    }

    @Test
    void findByEventIdReportsMissingAddress() {
        var eventId = UUID.randomUUID();
        when(repository.findProjectedByEventId(eventId)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class, () -> service.findByEventId(eventId));
    }

    @Test
    void deletesDistinctRootAndChildEventAddresses() {
        var rootId = UUID.randomUUID();
        var childId = UUID.randomUUID();

        service.deleteEventAddressesByEventIds(List.of(rootId, childId, rootId));

        verify(repository).deleteByEventId(rootId);
        verify(repository).deleteByEventId(childId);
    }

    @Test
    void rejectsDeleteWithoutValidEventIds() {
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteEventAddressesByEventIds(List.of()));
        verify(repository, never()).deleteByEventId(org.mockito.ArgumentMatchers.any());
    }
}
