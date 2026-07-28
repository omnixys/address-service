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
    public void handleCreate(KafkaEnvelope<?> envelope) {
        log.info("Processing authentication.create.addresses: {}", envelope);
        try {
            AddUserAddressesDTO dto = objectMapper.convertValue(
                    envelope.payload(),
                    AddUserAddressesDTO.class
            );
            userAddressService.createUserAddresses(dto);
            log.info("authentication.create.addresses completed: {}", dto);
        } catch (Exception e) {
            log.error("authentication.create.addresses failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaEvent(topic = "authentication.delete.addresses")
    public void handleDelete(KafkaEnvelope<?> envelope) {
        log.info("Processing authentication.delete.addresses: {}", envelope);
        try {
            DeleteUserAddressesDTO dto = objectMapper.convertValue(
                    envelope.payload(),
                    DeleteUserAddressesDTO.class
            );
            userAddressService.deleteUserAddressByUserId(dto.userId());
            log.info("authentication.delete.addresses completed: userId={}", dto.userId());
        } catch (Exception e) {
            log.error("authentication.delete.addresses failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}