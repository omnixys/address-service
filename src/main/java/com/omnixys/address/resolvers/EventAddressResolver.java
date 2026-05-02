package com.omnixys.address.resolvers;

import com.omnixys.address.models.inputs.CreateEventAddressDTO;
import com.omnixys.address.models.inputs.CreateEventAddressInput;
import com.omnixys.address.models.inputs.UpdateEventAddressInput;
import com.omnixys.address.models.payload.EventAddressPayload;
import com.omnixys.address.services.EventAddressService;
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
public class EventAddressResolver {

    private final EventAddressService service;


    @MutationMapping
    public EventAddressPayload createEventAddress(@Argument CreateEventAddressInput input) {
        return service.createEventAddress(input);
    }

    @MutationMapping
    public EventAddressPayload updateEventAddress(@Argument UpdateEventAddressInput input) {
        return service.updateEventAddress(input);
    }

    @MutationMapping
    public Boolean deleteEventAddressByEventId(@Argument UUID eventId) {
        return service.deleteEventAddressByEventId(eventId);
    }

    @QueryMapping
    public EventAddressPayload eventAddressById(@Argument UUID id) {
        return service.findById(id);
    }

    @QueryMapping
    public EventAddressPayload getEventAddressByEventId(@Argument UUID eventId) {
        log.debug("getEventAddressesByEventId: eventId={}", eventId);
        return service.findByEventId(eventId);
    }
}
