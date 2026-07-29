package com.omnixys.address.analytics;

import com.omnixys.context.ContextAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsOutboxService {
    private final AnalyticsOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void enqueue(
            String topic,
            String eventName,
            String aggregateType,
            UUID aggregateId,
            String subjectId,
            Map<String, Object> properties
    ) {
        var context = ContextAccessor.get();
        if (context == null || context.tenant() == null || !context.tenant().verified()) {
            throw new IllegalStateException("Verified tenant context is required for analytics facts");
        }
        var now = Instant.now();
        var record = new AnalyticsOutboxRecord();
        record.setId(UUID.randomUUID());
        record.setTopic(topic);
        record.setTenantId(context.tenant().tenantId());
        record.setActorId(context.principal() == null ? null : context.principal().actorId());
        record.setCorrelationId(context.correlationId());
        record.setOccurredAt(now);
        record.setNextAttemptAt(now);
        var payload = new LinkedHashMap<String, Object>();
        payload.put("producer", "address");
        payload.put("eventName", eventName);
        payload.put("occurredAt", now.toString());
        payload.put("aggregateId", aggregateId.toString());
        payload.put("aggregateType", aggregateType);
        if (subjectId != null && !subjectId.isBlank()) payload.put("subjectId", subjectId);
        payload.put("properties", properties);
        record.setPayload(objectMapper.writeValueAsString(payload));
        repository.save(record);
    }
}
