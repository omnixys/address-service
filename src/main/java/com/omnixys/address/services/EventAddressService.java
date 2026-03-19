package com.omnixys.address.services;

import com.omnixys.address.models.entity.EventAddress;
import com.omnixys.address.models.inputs.CreateEventAddressInput;
import com.omnixys.address.models.payload.EventAddressPayload;
import com.omnixys.address.repository.EventAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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

    @Transactional
    public void createEventAddress(CreateEventAddressInput input) {

        log.debug("Creating event address for eventId={}", input.eventId());

        var countryId = countryService.findByName(input.country()).getId();
        var stateId = stateService.findByNameAndCountryId(input.state(), countryId).getId();
        var cityId = cityService.findByNameAndStateId(input.city(), stateId).getId();
        var postalCodeId = postalCodeService.findByCodeAndCityId(input.postalCode(), cityId).getId();
        var streetId = streetService.findByNameAndCityId(input.street(), cityId).getId();
        var houseNumberId = houseNumberService.findByHouseNumberAndStreetId(input.houseNumber(), streetId).getId();

        EventAddress address = new EventAddress();
        address.setEventId(input.eventId());
        address.setCountryId(countryId);
        address.setStateId(stateId);
        address.setCityId(cityId);
        address.setPostalCodeId(postalCodeId);
        address.setStreetId(streetId);
        address.setHouseNumberId(houseNumberId);
        address.setAdditionalInfo(input.additionalInfo());

        repository.save(address);
    }

    @Transactional
    public void deleteEventAddressByEventId(UUID eventId) {

        log.debug("Deleting event addresses for eventId={}", eventId);

        repository.deleteByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public List<EventAddressPayload> findByEventId(UUID eventId) {
        log.debug("Fetching addresses for eventId={}", eventId);

        return repository.findPayloadByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public Optional<EventAddressPayload> findById(UUID id) {

        log.debug("Fetching address id={}", id);

        return repository.findById(id)
                .map(this::mapToPayload);
    }

    private EventAddressPayload mapToPayload(EventAddress address) {

        log.debug("Resolving address values for id={}", address.getId());

        String country = countryService.findById(address.getCountryId()).getName();

        String state = address.getStateId() != null
                ? stateService.findById(address.getStateId()).getName()
                : null;

        String city = cityService.findById(address.getCityId()).getName();

        String postalCode = address.getPostalCodeId() != null
                ? postalCodeService.findById(address.getPostalCodeId()).getCode()
                : null;

        String street = streetService.findById(address.getStreetId()).getName();

        String houseNumber = houseNumberService
                .findById(address.getHouseNumberId())
                .getNumber();

        return new EventAddressPayload(
                address.getId(),
                address.getEventId(),
                country,
                state,
                city,
                postalCode,
                street,
                houseNumber,
                address.getAdditionalInfo()
        );
    }
}