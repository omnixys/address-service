package com.omnixys.address.resolvers;

import com.omnixys.address.models.entity.UserAddress;
import com.omnixys.address.models.inputs.CreateUserAddressInput;
import com.omnixys.address.models.inputs.UpdateUserAddressInput;
import com.omnixys.address.models.inputs.UserAddressFilter;
import com.omnixys.address.models.payload.UserAddressPayload;
import com.omnixys.address.services.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserAddressResolver {

    private final UserAddressService service;


    @MutationMapping
    public UserAddress createUserAddress(@Argument @Valid CreateUserAddressInput input) {
        return service.createUserAddress(input);
    }

    @MutationMapping
    public UserAddress updateUserAddress(@Argument @Valid UpdateUserAddressInput input) {
        return service.updateUserAddress(input);
    }

    @MutationMapping
    public Boolean deleteUserAddressByUserId(@Argument UUID userId) {
        return service.deleteUserAddressByUserId(userId);
    }

    @QueryMapping
    public UserAddressPayload userAddressById(@Argument UUID id) {
        return service.findById(id).orElse(null);
    }

    @QueryMapping
    public List<UserAddressPayload> getUserAddressesByUserId(@Argument UUID userId) {
        log.debug("getUserAddressesByUserId: userId={}", userId);
        return service.findByUserId(userId);
    }

    @QueryMapping
    public List<UserAddressPayload> userAddresses(@Argument UserAddressFilter filter) {
        return service.find(filter);
    }
}
