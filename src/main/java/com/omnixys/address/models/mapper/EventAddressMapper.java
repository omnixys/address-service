package com.omnixys.address.models.mapper;

import com.omnixys.address.models.entity.EventAddress;
import com.omnixys.address.models.inputs.CreateEventAddressDTO;
import com.omnixys.address.models.payload.EventAddressPayload;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventAddressMapper {
    EventAddress toUserAddress(CreateEventAddressDTO createEventAddressInput);
    EventAddressPayload toPayload(EventAddress eventAddress);
}
