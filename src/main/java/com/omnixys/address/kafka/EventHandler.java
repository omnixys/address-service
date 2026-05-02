package com.omnixys.address.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    public void handleCreate(KafkaEnvelope<?> envelope) {;

        log.info("Processing event: {}", envelope);

        CreateEventAddressDTO dto = objectMapper.convertValue(
                envelope.payload(),
                CreateEventAddressDTO.class
        );

        eventAddressService.createEventAddress(dto);
    }

    @KafkaEvent(topic = "event.delete.address")
    public void handleDelete(KafkaEnvelope<?> envelope) {

        log.info("Processing event: {}", envelope);

        DeleteEventAddressDTO dto = objectMapper.convertValue(
                envelope.payload(),
                DeleteEventAddressDTO.class
        );

        eventAddressService.deleteEventAddressByEventId(dto.eventId());
    }
}