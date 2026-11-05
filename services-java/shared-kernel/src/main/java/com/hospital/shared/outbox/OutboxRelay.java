package com.hospital.shared.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox and publishes pending rows to Kafka. At-least-once by
 * design: consumers must be idempotent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Value("${hospital.outbox.topic:hospital.events}")
    private String topic;

    @Scheduled(fixedDelayString = "${hospital.outbox.relay-delay-ms:2000}")
    @Transactional
    public void relay() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            return;
        }
        List<OutboxEntry> pending = outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEntry entry : pending) {
            try {
                kafkaTemplate.send(topic, entry.getAggregateId(), entry.getPayload())
                        .get(5, TimeUnit.SECONDS);
                entry.setPublished(true);
                entry.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                log.warn("Outbox relay failed for event {} ({}): {}",
                        entry.getId(), entry.getEventType(), ex.getMessage());
                break;
            }
        }
    }
}
