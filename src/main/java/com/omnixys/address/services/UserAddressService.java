package com.omnixys.address.services;

import com.omnixys.address.exception.AddressNotFoundException;
import com.omnixys.address.models.dto.AddUserAddressesDTO;
import com.omnixys.address.models.entity.UserAddress;
import com.omnixys.address.models.inputs.CreateUserAddressInput;
import com.omnixys.address.models.inputs.UpdateUserAddressInput;
import com.omnixys.address.models.inputs.UserAddressFilter;
import com.omnixys.address.models.payload.UserAddressPayload;
import com.omnixys.address.repository.UserAddressRepository;
import com.omnixys.address.repository.UserAddressSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAddressService {

    private final UserAddressRepository repository;
    private final CountryService countryService;
    private final StateService stateService;
    private final CityService cityService;
    private final PostalCodeService postalCodeService;
    private final StreetService streetService;
    private final HouseNumberService houseNumberService;
    private final AddressCacheService addressCacheService;

//    private final KafkaPublisherService kafkaPublisherService;

    @Transactional
    public UserAddress createUserAddress(CreateUserAddressInput input) {

        log.debug("Creating user address for userId={}", input.userId());

        UserAddress address = new UserAddress();
        address.setUserId(input.userId());
        address.setCountryId(input.countryId());
        address.setStateId(input.stateId());
        address.setCityId(input.cityId());
        address.setPostalCodeId(input.postalCodeId());
        address.setStreetId(input.streetId());
        address.setHouseNumberId(input.houseNumberId());
        address.setAdditionalInfo(input.additionalInfo());
        address.setAddressType(input.addressType());

        return repository.save(address);
    }

    @Transactional
    public void createUserAddresses(final AddUserAddressesDTO dto) {

        log.debug("Creating addresses from signup token");

        var tokenData = addressCacheService.getSignupAddressToken(dto.token());

        var userId = dto.userId();

        List<UserAddress> result = new ArrayList<>();

        for (var addressItem : tokenData.addresses()) {

            var streetId = streetService.findByNameAndCityId(addressItem.street(), UUID.fromString(addressItem.cityId())).getId();
            var houseNumberId = houseNumberService.findByHouseNumberAndStreetId(addressItem.houseNumber(), streetId).getId();

            UserAddress address = new UserAddress();
            address.setUserId(userId);
            address.setCountryId(UUID.fromString(addressItem.countryId()));
            address.setStateId(UUID.fromString(addressItem.stateId()));
            address.setCityId(UUID.fromString(addressItem.cityId()));
            address.setPostalCodeId(UUID.fromString(addressItem.postalCodeId()));
            address.setStreetId(streetId);
            address.setHouseNumberId(houseNumberId);
            address.setAdditionalInfo(addressItem.additionalInfo());
            address.setAddressType(addressItem.addressType());

            result.add(repository.save(address));
        }

        log.debug("Added UserAddresses for UserId {}= {}", userId, result);

        addressCacheService.deleteToken(dto.token());
    }

    @Transactional
    public UserAddress updateUserAddress(UpdateUserAddressInput input) {

        log.debug("Updating user address id={}", input.id());

        UserAddress address = repository.findById(input.id())
                .orElseThrow(() -> new AddressNotFoundException(input.id()));

        if (input.addressType() != null) {
            address.setAddressType(input.addressType());
        }

        if (input.additionalInfo() != null) {
            address.setAdditionalInfo(input.additionalInfo());
        }

        return repository.save(address);
    }

    @Transactional
    public boolean deleteUserAddressByUserId(UUID userId) {

        log.debug("Deleting user addresses for userId={}", userId);

        repository.deleteByUserId(userId);

        return true;
    }

    @Transactional(readOnly = true)
    public List<UserAddressPayload> findByUserId(UUID userId) {
        log.debug("Fetching addresses for userId={}", userId);

        return repository.findPayloadByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<UserAddressPayload> findById(UUID id) {

        log.debug("Fetching address id={}", id);

        return repository.findById(id)
                .map(this::mapToPayload);
    }

    @Transactional(readOnly = true)
    public List<UserAddressPayload> find(UserAddressFilter filter) {

        Specification<UserAddress> spec =
                UserAddressSpecificationBuilder.build(filter);

        return repository.findAll(spec)
                .stream()
                .map(this::mapToPayload)
                .toList();
    }

    private UserAddressPayload mapToPayload(UserAddress address) {

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

        return new UserAddressPayload(
                address.getId(),
                address.getUserId(),
                country,
                state,
                city,
                postalCode,
                street,
                houseNumber,
                address.getAdditionalInfo(),
                address.getAddressType()
        );
    }

//    private void validateAddressReferences(final CreateUserAddressDTO dto) {
//        if (dto.countryId() == null) {
//            throw new IllegalArgumentException("countryId must not be null");
//        }
//
//        if (dto.cityId() == null) {
//            throw new IllegalArgumentException("cityId must not be null");
//        }
//
//        if (dto.addressType() == null) {
//            throw new IllegalArgumentException("addressType must not be null");
//        }
//
//        if (dto.stateId() != null && !repository.existsStateInCountry(dto.stateId(), dto.countryId())) {
//            throw new NotFoundException("State does not belong to country");
//        }
//
//        if (!repository.existsCityInCountry(dto.cityId(), dto.countryId())) {
//            throw new NotFoundException("City does not belong to country");
//        }
//
//        if (dto.postalCodeId() != null && !repository.existsPostalCodeInCity(dto.postalCodeId(), dto.cityId())) {
//            throw new NotFoundException("Postal code does not belong to city");
//        }
//
//        if (dto.streetId() != null && !repository.existsStreetInCity(dto.streetId(), dto.cityId())) {
//            throw new NotFoundException("Street does not belong to city");
//        }
//
//        if (dto.houseNumberId() != null && dto.streetId() == null) {
//            throw new IllegalArgumentException("houseNumberId requires streetId");
//        }
//
//        if (dto.houseNumberId() != null && !repository.existsHouseNumberInStreet(dto.houseNumberId(), dto.streetId())) {
//            throw new NotFoundException("House number does not belong to street");
//        }
//    }
}