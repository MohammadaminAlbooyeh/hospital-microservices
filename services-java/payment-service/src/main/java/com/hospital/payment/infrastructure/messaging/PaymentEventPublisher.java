package com.hospital.payment.infrastructure.messaging;

import com.hospital.payment.domain.model.Payment;
import com.hospital.shared.events.EventTypes;
import com.hospital.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final OutboxService outboxService;

    public void paymentCompleted(Payment payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId().toString());
        payload.put("invoiceId", payment.getInvoiceId().toString());
        payload.put("patientId", payment.getPatientId() == null ? null : payment.getPatientId().toString());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());
        payload.put("paidAt", (payment.getProcessedAt() == null ? Instant.now() : payment.getProcessedAt()).toString());
        outboxService.publish("Payment", payment.getId().toString(), EventTypes.PAYMENT_COMPLETED, payload);
    }

    public void paymentFailed(Payment payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId().toString());
        payload.put("invoiceId", payment.getInvoiceId().toString());
        payload.put("patientId", payment.getPatientId() == null ? null : payment.getPatientId().toString());
        payload.put("amount", payment.getAmount());
        payload.put("reason", payment.getFailureReason());
        payload.put("failedAt", (payment.getProcessedAt() == null ? Instant.now() : payment.getProcessedAt()).toString());
        outboxService.publish("Payment", payment.getId().toString(), EventTypes.PAYMENT_FAILED, payload);
    }
}
