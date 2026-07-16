package com.omnixys.address.models.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteEventAddressDTOTest {

    @Test
    void acceptsLegacySingleEventId() {
        var eventId = UUID.randomUUID();

        assertEquals(List.of(eventId), new DeleteEventAddressDTO(eventId, null).normalizedEventIds());
    }

    @Test
    void acceptsCanonicalEventIdsAndRemovesDuplicates() {
        var rootId = UUID.randomUUID();
        var childId = UUID.randomUUID();

        var result = new DeleteEventAddressDTO(rootId, List.of(rootId, childId)).normalizedEventIds();

        assertEquals(List.of(rootId, childId), result);
    }

    @Test
    void emptyPayloadDoesNotProduceNullIds() {
        assertTrue(new DeleteEventAddressDTO(null, null).normalizedEventIds().isEmpty());
    }
}
