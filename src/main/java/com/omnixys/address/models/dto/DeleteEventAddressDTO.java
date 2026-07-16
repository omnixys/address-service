package com.omnixys.address.models.dto;

import java.util.UUID;
import java.util.List;
import java.util.stream.Stream;

public record DeleteEventAddressDTO(
        UUID eventId,
        List<UUID> eventIds
) {
    public List<UUID> normalizedEventIds() {
        return Stream.concat(
                        eventId == null ? Stream.empty() : Stream.of(eventId),
                        eventIds == null ? Stream.empty() : eventIds.stream()
                )
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }
}
