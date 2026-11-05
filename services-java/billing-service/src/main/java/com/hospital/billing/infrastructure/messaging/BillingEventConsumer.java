package com.hospital.billing.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.billing.application.RecordPaymentOutcomeUseCase;
import com.hospital.shared.events.EventTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingEventConsumer {

    private final ObjectMapper objectMapper;
    private final RecordPaymentOutcomeUseCase recordPaymentOutcomeUseCase;

    @KafkaListener(
            topics = "${KAFKA_TOPIC:hospital.events}",
            groupId = "${spring.kafka.consumer.group-id:billing-service}")
    public void onMessage(String message) {
        JsonNode root;
        try {
            root = objectMapper.readTree(message);
        } catch (JsonProcessingException ex) {
            log.error("Discarding unparseable billing event: {}", ex.getMessage());
            return;
        }

        String eventType = text(root, "eventType", "event_type");
        if (eventType == null) {
            log.warn("Discarding billing event without eventType");
            return;
        }

        JsonNode payload = root.path("payload");
        if (EventTypes.PAYMENT_COMPLETED.equals(eventType)) {
            handlePaymentCompleted(payload);
        } else if (EventTypes.PAYMENT_FAILED.equals(eventType)) {
            handlePaymentFailed(payload);
        }
    }

    private void handlePaymentCompleted(JsonNode payload) {
        UUID invoiceId = uuid(text(payload, "invoiceId", "invoice_id"));
        if (invoiceId == null) {
            log.warn("PaymentCompleted event is missing invoiceId; ignoring");
            return;
        }
        UUID paymentId = uuid(text(payload, "paymentId", "payment_id"));
        Instant paidAt = instant(text(payload, "paidAt", "paid_at"));
        recordPaymentOutcomeUseCase.paymentCompleted(invoiceId, paymentId, paidAt);
        log.info("Invoice {} marked PAID by payment {}", invoiceId, paymentId);
    }

    private void handlePaymentFailed(JsonNode payload) {
        UUID invoiceId = uuid(text(payload, "invoiceId", "invoice_id"));
        if (invoiceId == null) {
            log.warn("PaymentFailed event is missing invoiceId; ignoring");
            return;
        }
        UUID paymentId = uuid(text(payload, "paymentId", "payment_id"));
        String reason = text(payload, "reason", "failureReason", "failure_reason");
        recordPaymentOutcomeUseCase.paymentFailed(invoiceId, paymentId, reason);
        log.info("Invoice {} compensated after failed payment {}", invoiceId, paymentId);
    }

    private String text(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                String asText = value.asText();
                if (!asText.isBlank()) {
                    return asText;
                }
            }
        }
        return null;
    }

    private UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private Instant instant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
