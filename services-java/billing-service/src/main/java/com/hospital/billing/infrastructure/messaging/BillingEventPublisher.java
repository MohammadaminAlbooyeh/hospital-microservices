package com.hospital.billing.infrastructure.messaging;

import com.hospital.billing.domain.model.Invoice;
import com.hospital.shared.events.EventTypes;
import com.hospital.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BillingEventPublisher {

    private final OutboxService outboxService;

    public void invoiceIssued(Invoice invoice) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("invoiceId", invoice.getId().toString());
        payload.put("encounterId", invoice.getEncounterId() == null ? null : invoice.getEncounterId().toString());
        payload.put("patientId", invoice.getPatientId().toString());
        payload.put("totalAmount", invoice.getTotalAmount());
        payload.put("patientShare", invoice.getPatientShare());
        payload.put("insurerShare", invoice.getInsurerShare());
        payload.put("currency", invoice.getCurrency());
        payload.put("issuedAt", invoice.getIssuedAt().toString());
        outboxService.publish("Invoice", invoice.getId().toString(), EventTypes.INVOICE_ISSUED, payload);
    }
}
