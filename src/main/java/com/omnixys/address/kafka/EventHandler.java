package com.omnixys.address.kafka;

import tools.jackson.databind.ObjectMapper;
import com.omnixys.address.models.dto.DeleteEventAddressDTO;
import com.omnixys.address.models.inputs.CreateEventAddressDTO;
import com.omnixys.address.services.EventAddressService;
import com.omnixys.context.ContextAccessor;
import com.omnixys.context.ContextSnapshot;
import com.omnixys.context.TenantContext;
import com.omnixys.kafka.annotation.KafkaEvent;
import com.omnixys.kafka.model.KafkaEnvelope;
import com.omnixys.logger.logging.OmnixysLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventHandler {

    private final EventAddressService eventAddressService;
    private final ObjectMapper objectMapper;
    private final OmnixysLogger log;

    @KafkaEvent(topic = "event.create.address")
    public void handleCreate(KafkaEnvelope<?> envelope, Map<String, String> headers) {
        log.info("Processing event.create.address: {}", envelope);
        try {
            withVerifiedTenant(headers, () -> {
                CreateEventAddressDTO dto = objectMapper.convertValue(
                        envelope.payload(),
                        CreateEventAddressDTO.class
                );
                eventAddressService.createEventAddress(dto);
            });
            log.info("event.create.address completed: {}", envelope.payload());
        } catch (Exception e) {
            log.error("event.create.address failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaEvent(topic = "event.delete.address")
    public void handleDelete(KafkaEnvelope<?> envelope, Map<String, String> headers) {
        log.info("Processing event.delete.address: {}", envelope);
        try {
            withVerifiedTenant(headers, () -> {
                DeleteEventAddressDTO dto = objectMapper.convertValue(
                        envelope.payload(),
                        DeleteEventAddressDTO.class
                );
                eventAddressService.deleteEventAddressesByEventIds(dto.normalizedEventIds());
            });
            log.info("event.delete.address completed: {}", envelope.payload());
        } catch (Exception e) {
            log.error("event.delete.address failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void withVerifiedTenant(Map<String, String> headers, Runnable action) {
        String tenantId = headers.get("x-tenant-id");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = headers.get("x-meta-tenantId");
        }
        ContextSnapshot original = ContextAccessor.get();
        if (tenantId != null && !tenantId.isBlank()) {
            ContextSnapshot snapshot = original != null
                    ? new ContextSnapshot(
                            original.requestId(),
                            original.correlationId(),
                            original.startedAtEpochMs(),
                            new TenantContext(tenantId, "kafka-header", true),
                            original.principal(),
                            original.client(),
                            original.transport(),
                            original.trace()
                    )
                    : new ContextSnapshot(
                            headers.getOrDefault("x-request-id", "unscoped"),
                            headers.getOrDefault("x-correlation-id", "unscoped"),
                            System.currentTimeMillis(),
                            new TenantContext(tenantId, "kafka-header", true),
                            null,
                            new com.omnixys.context.ClientMetadata(
                                    null, null, null, null, null, null, null, null, null),
                            new com.omnixys.context.TransportMetadata(
                                    "kafka", null, null, null, null, null,
                                    null, null, null, null, null, null, null),
                            null
                    );
            ContextAccessor.set(snapshot);
        }
        try {
            action.run();
        } finally {
            if (original != null) {
                ContextAccessor.set(original);
            } else {
                ContextAccessor.clear();
            }
        }
    }
}
