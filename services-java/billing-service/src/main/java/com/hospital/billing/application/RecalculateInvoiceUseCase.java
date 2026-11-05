package com.hospital.billing.application;

import com.hospital.billing.domain.model.Invoice;
import com.hospital.billing.domain.service.InvoiceDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecalculateInvoiceUseCase {

    private final InvoiceDomainService invoiceDomainService;

    @Transactional
    public Invoice execute(UUID invoiceId) {
        return invoiceDomainService.recalculate(invoiceId);
    }
}
