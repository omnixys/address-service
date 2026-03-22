package com.omnixys.address.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnixys.address.models.dto.AddUserAddressesDTO;
import com.omnixys.address.models.dto.DeleteAddressesDTO;
import com.omnixys.address.services.UserAddressService;
import com.omnixys.kafka.annotation.KafkaEvent;
import com.omnixys.kafka.model.KafkaEnvelope;
import com.omnixys.logger.logging.OmnixysLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthenticationHandler {

    private final UserAddressService userAddressService;
    private final ObjectMapper objectMapper;
    private final OmnixysLogger log;

    @KafkaEvent(topic = "authentication.create.addresses")
    public void handleCreate(KafkaEnvelope<?> envelope) {;

        log.info("Processing event: {}", envelope);

        AddUserAddressesDTO dto = objectMapper.convertValue(
                envelope.payload(),
                AddUserAddressesDTO.class
        );

        userAddressService.createUserAddresses(dto);
    }

    @KafkaEvent(topic = "authentication.delete.addresses")
    public void handleDelete(KafkaEnvelope<?> envelope) {

        log.info("Processing event: {}", envelope);

        DeleteAddressesDTO dto = objectMapper.convertValue(
                envelope.payload(),
                DeleteAddressesDTO.class
        );

        userAddressService.deleteUserAddressByUserId(dto.userId());
    }
}