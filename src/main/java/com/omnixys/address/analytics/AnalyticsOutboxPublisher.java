package com.omnixys.address.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsOutboxPublisher {
    private static final int MAX_ATTEMPTS = 10;
    private final AnalyticsOutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.analytics.outbox.poll-interval:1000}")
    @Transactional
    public void publishReady() {
        for (var record : repository
                .findTop50ByPublishedAtIsNullAndDeadLetteredAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(
                        Instant.now())) publish(record);
    }

    private void publish(AnalyticsOutboxRecord record) {
        try {
            var envelope = Map.of(
                    "eventId", record.getId().toString(),
                    "eventName", record.getTopic(),
                    "eventType", "EVENT",
                    "eventVersion", "1",
                    "service", "address",
                    "timestamp", record.getOccurredAt().toString(),
                    "payload", objectMapper.readValue(record.getPayload(), Object.class)
            );
            var message = new ProducerRecord<String, String>(
                    record.getTopic(), record.getId().toString(), objectMapper.writeValueAsString(envelope));
            addHeader(message, "x-tenant-id", record.getTenantId());
            addHeader(message, "x-correlation-id", record.getCorrelationId());
            addHeader(message, "x-actor-id", record.getActorId());
            kafka.send(message).get();
            record.setPublishedAt(Instant.now());
            record.setLastError(null);
        } catch (Exception error) {
            var attempts = record.getAttempts() + 1;
            record.setAttempts(attempts);
            record.setLastError(error.getMessage());
            if (attempts >= MAX_ATTEMPTS) {
                record.setDeadLetteredAt(Instant.now());
            } else {
                record.setNextAttemptAt(
                        Instant.now().plus(Duration.ofSeconds(Math.min(300, 1L << Math.min(attempts, 8)))));
            }
            log.warn("Analytics outbox publish failed id={} attempt={}", record.getId(), attempts, error);
        }
    }

    private static void addHeader(ProducerRecord<String, String> message, String name, String value) {
        if (value != null && !value.isBlank()) {
            message.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
