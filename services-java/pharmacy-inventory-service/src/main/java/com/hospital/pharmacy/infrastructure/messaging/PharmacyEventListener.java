package com.hospital.pharmacy.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.hospital.pharmacy.application.PaymentCompensationUseCase;
import com.hospital.shared.events.EventTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PharmacyEventListener {

    private static final Set<String> IDENTIFIER_FIELDS = Set.of(
            "paymentid",
            "invoiceid",
            "invoicenumber",
            "reference",
            "idempotencykey",
            "dispenseid",
            "prescriptionid",
            "encounterid",
            "paymentreference",
            "invoicereference",
            "transactionreference");

    private final ObjectMapper objectMapper;
    private final PaymentCompensationUseCase paymentCompensationUseCase;

    @KafkaListener(topics = "${KAFKA_TOPIC:hospital.events}", groupId = "pharmacy-inventory-service")
    public void onEvent(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = text(root, "eventType", "event_type");
            if (eventType == null) {
                log.debug("Ignoring pharmacy event without an event type");
                return;
            }

            Set<String> references = collectReferences(payload(root));
            String aggregateId = text(root, "aggregateId", "aggregate_id");
            if (aggregateId != null) {
                references.add(aggregateId);
            }

            switch (eventType) {
                case EventTypes.PAYMENT_COMPLETED ->
                        paymentCompensationUseCase.finalizePayment(references);
                case EventTypes.PAYMENT_FAILED ->
                        paymentCompensationUseCase.compensateFailedPayment(references);
                default -> log.debug("Ignoring unrelated event type {}", eventType);
            }
        } catch (Exception ex) {
            log.warn("Discarding malformed or unprocessable pharmacy event: {}", ex.getMessage());
        }
    }

    private JsonNode payload(JsonNode root) {
        JsonNode payload = root.get("payload");
        if (payload == null || payload.isNull()) {
            return MissingNode.getInstance();
        }
        if (payload.isTextual()) {
            try {
                return objectMapper.readTree(payload.asText());
            } catch (Exception ex) {
                return MissingNode.getInstance();
            }
        }
        return payload;
    }

    private Set<String> collectReferences(JsonNode node) {
        Set<String> references = new LinkedHashSet<>();
        if (node == null || !node.isObject()) {
            return references;
        }
        node.fields().forEachRemaining(field -> {
            String normalised = field.getKey()
                    .replace("_", "")
                    .replace("-", "")
                    .toLowerCase(Locale.ROOT);
            if (!IDENTIFIER_FIELDS.contains(normalised)) {
                return;
            }
            JsonNode value = field.getValue();
            if (value != null && (value.isTextual() || value.isNumber())) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    references.add(text.trim());
                }
            }
        });
        return references;
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isValueNode() && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }
}
