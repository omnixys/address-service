package com.omnixys.address.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnixys.address.models.dto.AddUserAddressesDTO;
import com.omnixys.address.models.dto.DeleteAddressesDTO;
import com.omnixys.address.models.dto.TestEvent;
import com.omnixys.address.models.inputs.CreateEventAddressInput;
import com.omnixys.address.services.EventAddressService;
import com.omnixys.address.services.UserAddressService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;
    private final UserAddressService userAddressService;
    private final EventAddressService eventAddressService;
    private final Tracer tracer;

    // =========================================================
    // LISTENERS
    // =========================================================

    @KafkaListener(topics = "authentication.create.addresses", groupId = "${app.groupId}")
    public void consumeCreateUserAddress(byte[] payload, ConsumerRecord<String, byte[]> record) {
        handle(record, payload, () -> {
            EventEnvelope<AddUserAddressesDTO> event =
                    objectMapper.readValue(payload, new TypeReference<>() {});
            userAddressService.createUserAddresses(event.payload());
        });
    }

    @KafkaListener(topics = "authentication.delete.addresses", groupId = "${app.groupId}")
    public void consumeDeleteUserAddress(byte[] payload, ConsumerRecord<String, byte[]> record) {
        handle(record, payload, () -> {
            EventEnvelope<DeleteAddressesDTO> event =
                    objectMapper.readValue(payload, new TypeReference<>() {});
            userAddressService.deleteUserAddressByUserId(event.payload().id());
        });
    }

    @KafkaListener(topics = "event.create.address", groupId = "${app.groupId}")
    public void consumeCreateEventAddress(byte[] payload, ConsumerRecord<String, byte[]> record) {
        handle(record, payload, () -> {
            EventEnvelope<CreateEventAddressInput> event =
                    objectMapper.readValue(payload, new TypeReference<>() {});
            eventAddressService.createEventAddress(event.payload());
        });
    }

    @KafkaListener(topics = "event.delete.address", groupId = "${app.groupId}")
    public void consumeDeleteEventAddress(byte[] payload, ConsumerRecord<String, byte[]> record) {
        handle(record, payload, () -> {
            EventEnvelope<DeleteAddressesDTO> event =
                    objectMapper.readValue(payload, new TypeReference<>() {});
            eventAddressService.deleteEventAddressByEventId(event.payload().id());
        });
    }


    @KafkaListener(topics = "test", groupId = "${app.groupId}")
    public void consumeTest(byte[] payload, ConsumerRecord<String, byte[]> record) {
        handle(record, payload, () -> {
            TestEvent event = objectMapper.readValue(payload, TestEvent.class);
            log.info("Test event received: {}", event.message());
        });
    }

    // =========================================================
    // CORE PROCESSING PIPELINE
    // =========================================================

    private void handle(ConsumerRecord<String, byte[]> record, byte[] payload, ThrowingRunnable logic) {
        Context extractedContext = extractContext(record.headers());

        Span span = startConsumerSpan(record, extractedContext);

        Context processingContext = extractedContext.with(span);

        try (Scope scope = processingContext.makeCurrent()) {

            logTraceDebug(record, extractedContext, span);

            logic.run();

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            log.error("Kafka processing failed", e);
        } finally {
            span.end();
        }
    }

    // =========================================================
    // CONTEXT EXTRACTION
    // =========================================================

    private Context extractContext(Headers headers) {
        return GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.root(), headers, buildHeaderGetter());
    }

    // =========================================================
    // SPAN CREATION
    // =========================================================

    private Span startConsumerSpan(ConsumerRecord<String, byte[]> record, Context parentContext) {
        return tracer.spanBuilder("kafka.consume " + record.topic())
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(parentContext)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.operation", "receive")
                .setAttribute("messaging.destination.name", record.topic())
                .setAttribute("messaging.kafka.partition", record.partition())
                .setAttribute("messaging.kafka.offset", record.offset())
                .startSpan();
    }

    // =========================================================
    // HEADER GETTER
    // =========================================================

    private TextMapGetter<Headers> buildHeaderGetter() {
        return new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Headers carrier) {
                return carrier == null
                        ? java.util.List.of()
                        : java.util.Arrays.stream(carrier.toArray()).map(Header::key).toList();
            }

            @Override
            public String get(Headers carrier, String key) {
                if (carrier == null) return null;
                Header header = carrier.lastHeader(key);
                return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
            }
        };
    }

    // =========================================================
    // TRACE DEBUGGING
    // =========================================================

    private void logTraceDebug(ConsumerRecord<String, byte[]> record,
                               Context extractedContext,
                               Span span) {

        Span parentSpan = Span.fromContext(extractedContext);

        Map<String, Object> debug = new HashMap<>();
        debug.put("topic", record.topic());
        debug.put("partition", record.partition());
        debug.put("offset", record.offset());
        debug.put("headers", extractHeaders(record.headers()));

        debug.put("extractedContext", Map.of(
                "traceId", parentSpan.getSpanContext().getTraceId(),
                "spanId", parentSpan.getSpanContext().getSpanId(),
                "valid", parentSpan.getSpanContext().isValid(),
                "remote", parentSpan.getSpanContext().isRemote()
        ));

        debug.put("consumerSpan", Map.of(
                "traceId", span.getSpanContext().getTraceId(),
                "spanId", span.getSpanContext().getSpanId()
        ));

        debug.put("relation", Map.of(
                "sameTrace", span.getSpanContext().getTraceId()
                        .equals(parentSpan.getSpanContext().getTraceId()),
                "parentSpanId", parentSpan.getSpanContext().getSpanId()
        ));

        log.info("TRACE_DEBUG {}", debug);
    }

    private Map<String, String> extractHeaders(Headers headers) {
        Map<String, String> map = new HashMap<>();
        headers.forEach(h ->
                map.put(h.key(), new String(h.value(), StandardCharsets.UTF_8))
        );
        return map;
    }

    // =========================================================
    // FUNCTIONAL INTERFACE
    // =========================================================

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}