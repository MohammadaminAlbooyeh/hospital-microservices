package com.hospital.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Transport envelope written to the outbox. Both Java and Python consumers
 * understand this shape.
 */
public record EventEnvelope(
        String eventId,
        String eventType,
        Instant occurredAt,
        String aggregateId,
        Object payload,
        String correlationId
) {

    public static EventEnvelope of(String eventType, String aggregateId, Object payload) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                aggregateId,
                payload,
                null
        );
    }

    public static EventEnvelope of(String eventType, String aggregateId, Object payload, String correlationId) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                aggregateId,
                payload,
                correlationId
        );
    }
}
