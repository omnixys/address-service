package com.omnixys.address.kafka;

public record EventEnvelope<T>(
        String service,
        String event,
        String version,
        T payload
) {}