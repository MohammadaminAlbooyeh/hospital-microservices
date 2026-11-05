package com.hospital.shared.events;

import java.time.Instant;

/**
 * Marker for an event produced by a service. Events are serialised into the
 * transactional outbox and relayed to Kafka.
 */
public interface DomainEvent {

    String eventId();

    String eventType();

    Instant occurredAt();

    String aggregateId();

    Object payload();
}
