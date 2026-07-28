package com.omnixys.address.kafka;

import tools.jackson.databind.ObjectMapper;
import com.omnixys.address.models.dto.DeleteEventAddressDTO;
import com.omnixys.address.models.dto.DeleteUserAddressesDTO;
import com.omnixys.address.models.inputs.CreateEventAddressDTO;
import com.omnixys.address.services.EventAddressService;
import com.omnixys.kafka.annotation.KafkaEvent;
import com.omnixys.kafka.model.KafkaEnvelope;
import com.omnixys.logger.logging.OmnixysLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventHandler {

    private final EventAddressService eventAddressService;
    private final ObjectMapper objectMapper;
    private final OmnixysLogger log;

    @KafkaEvent(topic = "event.create.address")
    public void handleCreate(KafkaEnvelope<?> envelope) {
        log.info("Processing event.create.address: {}", envelope);
        try {
            CreateEventAddressDTO dto = objectMapper.convertValue(
                    envelope.payload(),
                    CreateEventAddressDTO.class
            );
            eventAddressService.createEventAddress(dto);
            log.info("event.create.address completed: {}", dto);
        } catch (Exception e) {
            log.error("event.create.address failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaEvent(topic = "event.delete.address")
    public void handleDelete(KafkaEnvelope<?> envelope) {
        log.info("Processing event.delete.address: {}", envelope);
        try {
            DeleteEventAddressDTO dto = objectMapper.convertValue(
                    envelope.payload(),
                    DeleteEventAddressDTO.class
            );
            eventAddressService.deleteEventAddressesByEventIds(dto.normalizedEventIds());
            log.info("event.delete.address completed: eventIds={}", dto.normalizedEventIds());
        } catch (Exception e) {
            log.error("event.delete.address failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
