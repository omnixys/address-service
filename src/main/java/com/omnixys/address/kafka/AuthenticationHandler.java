package com.omnixys.address.kafka;

import tools.jackson.databind.ObjectMapper;
import com.omnixys.address.models.dto.AddUserAddressesDTO;
import com.omnixys.address.models.dto.DeleteUserAddressesDTO;
import com.omnixys.address.services.UserAddressService;
import com.omnixys.kafka.annotation.KafkaEvent;
import com.omnixys.kafka.model.KafkaEnvelope;
import com.omnixys.logger.logging.OmnixysLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

        DeleteUserAddressesDTO dto = objectMapper.convertValue(
                envelope.payload(),
                DeleteUserAddressesDTO.class
        );

        userAddressService.deleteUserAddressByUserId(dto.userId());
    }
}