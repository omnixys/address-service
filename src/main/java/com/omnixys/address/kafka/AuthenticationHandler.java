package com.omnixys.address.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnixys.address.models.dto.AddUserAddressesDTO;
import com.omnixys.address.models.dto.DeleteAddressesDTO;
import com.omnixys.address.services.UserAddressService;
import com.omnixys.kafka.annotation.KafkaEvent;
import com.omnixys.kafka.envelope.KafkaEnvelope;
import com.omnixys.observability.logging.OmnixysLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthenticationHandler {

    private final UserAddressService userAddressService;
    private final ObjectMapper objectMapper;
    private final OmnixysLogger logger;

    @KafkaEvent(topic = "authentication.create.addresses")
    public void handleCreate(KafkaEnvelope<?> envelope) {
        var log = logger.child("AuhenticationHandler#handleCreate");

        log.info("Processing event:", Map.of("event", envelope));

        AddUserAddressesDTO dto = objectMapper.convertValue(
                envelope.payload(),
                AddUserAddressesDTO.class
        );

        userAddressService.createUserAddresses(dto);
    }

    @KafkaEvent(topic = "authentication.delete.addresses")
    public void handleDelete(KafkaEnvelope<?> envelope) {
        var log = logger.child("AuhenticationHandler#handledelete");

        log.info("Processing event:", Map.of("event", envelope));

        DeleteAddressesDTO dto = objectMapper.convertValue(
                envelope.payload(),
                DeleteAddressesDTO.class
        );

        userAddressService.deleteUserAddressByUserId(dto.userId());
    }
}