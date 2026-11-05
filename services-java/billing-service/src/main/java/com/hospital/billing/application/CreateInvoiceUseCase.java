package com.hospital.billing.application;

import com.hospital.billing.api.dto.CreateInvoiceRequest;
import com.hospital.billing.api.mapper.InvoiceMapper;
import com.hospital.billing.domain.model.Invoice;
import com.hospital.billing.domain.service.InvoiceDomainService;
import com.hospital.billing.infrastructure.messaging.BillingEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateInvoiceUseCase {

    private final InvoiceDomainService invoiceDomainService;
    private final BillingEventPublisher eventPublisher;

    @Transactional
    public Invoice execute(CreateInvoiceRequest request) {
        Invoice invoice = invoiceDomainService.create(InvoiceMapper.toNewEntity(request));
        eventPublisher.invoiceIssued(invoice);
        return invoice;
    }
}
