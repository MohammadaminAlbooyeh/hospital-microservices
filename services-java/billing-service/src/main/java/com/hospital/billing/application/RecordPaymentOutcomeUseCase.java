package com.hospital.billing.application;

import com.hospital.billing.domain.model.Invoice;
import com.hospital.billing.domain.service.InvoiceDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordPaymentOutcomeUseCase {

    private final InvoiceDomainService invoiceDomainService;

    @Transactional
    public Invoice paymentCompleted(UUID invoiceId, UUID paymentId, Instant paidAt) {
        return invoiceDomainService.applyPaymentCompleted(invoiceId, paymentId, paidAt);
    }

    @Transactional
    public Invoice paymentFailed(UUID invoiceId, UUID paymentId, String reason) {
        return invoiceDomainService.applyPaymentFailed(invoiceId, paymentId, reason);
    }
}
