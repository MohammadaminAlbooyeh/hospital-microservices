package com.hospital.billing.unit;

import com.hospital.billing.domain.model.Invoice;
import com.hospital.billing.domain.model.InvoiceLine;
import com.hospital.billing.domain.model.InvoiceLineCategory;
import com.hospital.billing.domain.model.InvoiceStatus;
import com.hospital.billing.domain.repository.InvoiceRepository;
import com.hospital.billing.domain.service.InvoiceDomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceDomainServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceDomainService invoiceDomainService;

    @Test
    void calculatesInsurerAndPatientShares() {
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice invoice = Invoice.builder()
                .patientId(UUID.randomUUID())
                .currency("usd")
                .insuranceCoveragePercent(new BigDecimal("60"))
                .build();
        invoice.addLine(line(InvoiceLineCategory.LAB, "Complete blood count", 2, "50.00"));
        invoice.addLine(line(InvoiceLineCategory.BED, "Ward bed", 1, "200.00"));

        Invoice saved = invoiceDomainService.create(invoice);

        assertThat(saved.getSubtotal()).isEqualByComparingTo("300.00");
        assertThat(saved.getInsurerShare()).isEqualByComparingTo("180.00");
        assertThat(saved.getPatientShare()).isEqualByComparingTo("120.00");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getLines()).extracting(InvoiceLine::getAmount)
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("200.00"));
    }

    @Test
    void marksInvoicePaidWhenPaymentCompletes() {
        UUID invoiceId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant paidAt = Instant.parse("2026-01-01T10:15:30Z");
        invoiceDomainService.applyPaymentCompleted(invoiceId, paymentId, paidAt);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getPaidAt()).isEqualTo(paidAt);
        assertThat(invoice.getLastPaymentId()).isEqualTo(paymentId);
        assertThat(invoice.getFailureReason()).isNull();
    }

    @Test
    void ignoresDuplicatePaymentCompletedEvent() {
        UUID invoiceId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        invoice.setLastPaymentId(paymentId);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        invoiceDomainService.applyPaymentCompleted(invoiceId, paymentId, Instant.now());

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void appliesPaymentFailureCompensationExactlyOnce() {
        UUID invoiceId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invoiceDomainService.applyPaymentFailed(invoiceId, paymentId, "declined by gateway");
        invoiceDomainService.applyPaymentFailed(invoiceId, paymentId, "declined by gateway");

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(invoice.getFailureReason()).isEqualTo("declined by gateway");
        assertThat(invoice.getLastPaymentId()).isEqualTo(paymentId);
        verify(invoiceRepository, times(1)).save(invoice);
    }

    private Invoice invoice(UUID id, InvoiceStatus status) {
        Invoice invoice = Invoice.builder()
                .patientId(UUID.randomUUID())
                .currency("USD")
                .insuranceCoveragePercent(BigDecimal.ZERO)
                .status(status)
                .subtotal(new BigDecimal("300.00"))
                .insurerShare(BigDecimal.ZERO)
                .patientShare(new BigDecimal("300.00"))
                .totalAmount(new BigDecimal("300.00"))
                .build();
        invoice.setId(id);
        return invoice;
    }

    private InvoiceLine line(InvoiceLineCategory category, String description, int quantity, String unitPrice) {
        return InvoiceLine.builder()
                .category(category)
                .description(description)
                .quantity(quantity)
                .unitPrice(new BigDecimal(unitPrice))
                .build();
    }
}
