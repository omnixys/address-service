package com.omnixys.address.kafka;

public record TraceContextDTO(
        String traceId,
        String spanId,
        String parentSpanId,
        String sampled
) {}