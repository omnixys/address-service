package com.omnixys.address.services;

import com.omnixys.address.exception.AddressNotFoundException;
import com.omnixys.address.models.entity.EventAddress;
import com.omnixys.address.models.inputs.CreateEventAddressDTO;
import com.omnixys.address.models.inputs.CreateEventAddressInput;
import com.omnixys.address.models.inputs.UpdateEventAddressInput;
import com.omnixys.address.models.payload.EventAddressPayload;
import com.omnixys.address.repository.EventAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventAddressService {

    private final EventAddressRepository repository;
    private final CountryService countryService;
    private final StateService stateService;
    private final CityService cityService;
    private final PostalCodeService postalCodeService;
    private final StreetService streetService;
    private final HouseNumberService houseNumberService;

    /**
     * Creates a new EventAddress using a strict "find-or-create" strategy.
     * This guarantees that address components always exist and prevents runtime failures.
     */
    public void createEventAddress(CreateEventAddressDTO input) {

        log.debug("Creating event address for eventId={}", input.eventId());

        var resolved = resolveAddress(input);

        EventAddress address = EventAddress.builder()
                .eventId(input.eventId())
                .countryId(resolved.countryId())
                .stateId(resolved.stateId())
                .cityId(resolved.cityId())
                .postalCodeId(resolved.postalCodeId())
                .streetId(resolved.streetId())
                .houseNumberId(resolved.houseNumberId())
                .additionalInfo(input.additionalInfo())
                .build();

        repository.save(address);
    }

    public EventAddressPayload createEventAddress(CreateEventAddressInput input) {

        log.debug("Creating event address for eventId={}", input.eventId());

        EventAddress address = EventAddress.builder()
                .eventId(input.eventId())
                .countryId(input.countryId())
                .stateId(input.stateId())
                .cityId(input.cityId())
                .postalCodeId(input.postalCodeId())
                .streetId(input.streetId())
                .houseNumberId(input.houseNumberId())
                .additionalInfo(input.additionalInfo())
                .build();

        repository.save(address);

        return getSinglePayload(address.getId());
    }

    /**
     * Fully updates an address.
     * Re-resolves all address components to ensure consistency.
     */
    public EventAddressPayload updateEventAddress(UpdateEventAddressInput input) {

        log.debug("Updating event address id={}", input.id());

        EventAddress address = repository.findById(input.id())
                .orElseThrow(() -> new AddressNotFoundException(input.id()));

        var streetId = streetService.findByNameAndCityId(input.street(), input.cityId()).getId();
        var houseNumberId = houseNumberService.findByHouseNumberAndStreetId(input.houseNumber(), streetId).getId();

        address.setCountryId(input.countryId());
        address.setStateId(input.stateId());
        address.setCityId(input.cityId());
        address.setPostalCodeId(input.postalCodeId());
        address.setStreetId(streetId);
        address.setHouseNumberId(houseNumberId);
        address.setAdditionalInfo(input.additionalInfo());

        repository.save(address);

        return getSinglePayload(address.getId());
    }


    public Boolean deleteEventAddressByEventId(UUID eventId) {
        log.debug("Deleting event addresses for eventId={}", eventId);
        repository.deleteByEventId(eventId);
        return true;
    }

    @Transactional(readOnly = true)
    public EventAddressPayload findById(UUID id) {
        var payload = getSinglePayload(id);
        log.debug(String.valueOf(payload));
        return payload;
    }

    @Transactional(readOnly = true)
    public EventAddressPayload findByEventId(UUID eventId) {

        return repository.findRawByEventId(eventId)
                .map(this::mapSingle)
                .orElseThrow(() -> new AddressNotFoundException(eventId));
    }

    /**
     * Centralized resolution logic.
     * Ensures consistent hierarchy resolution and avoids duplication.
     */
    private ResolvedAddress resolveAddress(CreateEventAddressDTO input) {
        var countryId = countryService.findByName(input.country()).getId();
        var stateId = stateService.findByNameAndCountryId(input.state(), countryId).getId();
        var cityId = cityService.findByNameAndStateId(input.city(), stateId).getId();
        var postalCodeId = postalCodeService.findByCodeAndCityId(input.postalCode(), cityId).getId();
        var streetId = streetService.findByNameAndCityId(input.street(), cityId).getId();
        var houseNumberId = houseNumberService.findByHouseNumberAndStreetId(input.houseNumber(), streetId).getId();

        return new ResolvedAddress(
                countryId,
                stateId,
                cityId,
                postalCodeId,
                streetId,
                houseNumberId
        );
    }

    /**
     * Internal immutable structure for resolved IDs.
     */
    private record ResolvedAddress(
            UUID countryId,
            UUID stateId,
            UUID cityId,
            UUID postalCodeId,
            UUID streetId,
            UUID houseNumberId
    ) {}


    /**
     * Fetches a fully resolved payload via projection query.
     * Avoids N+1 and guarantees consistent response structure.
     */
    private EventAddressPayload getSinglePayload(UUID id) {

        return repository.findById(id)
                .map(a -> repository.findRawByEventId(a.getEventId())
                        .map(this::mapSingle)
                        .orElseThrow(() -> new AddressNotFoundException(id)))
                .orElseThrow(() -> new AddressNotFoundException(id));
    }

    public EventAddressPayload mapSingle(Object[] r) {

        // 🔥 FIX: unwrap nested result
        if (r.length == 1 && r[0] instanceof Object[]) {
            r = (Object[]) r[0];
        }

        return new EventAddressPayload(
                (UUID) r[0],
                (UUID) r[1],
                (String) r[2],
                (String) r[3],
                (String) r[4],
                (String) r[5],
                (String) r[6],
                (String) r[7],
                (String) r[8],
                r[9] != null ? ((Number) r[9]).doubleValue() : null,
                r[10] != null ? ((Number) r[10]).doubleValue() : null
        );
    }
}