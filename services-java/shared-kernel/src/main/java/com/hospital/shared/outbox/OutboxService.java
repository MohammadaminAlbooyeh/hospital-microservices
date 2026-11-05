package com.hospital.shared.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.shared.events.EventEnvelope;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enqueues domain events into the outbox inside the caller's transaction.
 * Call this from the same use-case transaction as the state change.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(String aggregateType, String aggregateId, String eventType, Object payload) {
        EventEnvelope envelope = EventEnvelope.of(eventType, aggregateId, payload, MDC.get("correlationId"));
        try {
            String json = objectMapper.writeValueAsString(envelope);
            OutboxEntry entry = OutboxEntry.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(json)
                    .published(false)
                    .build();
            outboxRepository.save(entry);
        } catch (JsonProcessingException ex) {
            throw ApiException.badRequest("Unable to serialise event " + eventType);
        }
    }
}
