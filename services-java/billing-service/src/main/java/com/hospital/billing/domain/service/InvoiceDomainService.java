package com.hospital.billing.domain.service;

import com.hospital.billing.domain.model.Invoice;
import com.hospital.billing.domain.model.InvoiceLine;
import com.hospital.billing.domain.model.InvoiceStatus;
import com.hospital.billing.domain.repository.InvoiceRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceDomainService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice create(Invoice invoice) {
        if (invoice.getPatientId() == null) {
            throw ApiException.badRequest("patientId is required");
        }
        if (invoice.getCurrency() == null || invoice.getCurrency().isBlank()) {
            throw ApiException.badRequest("currency is required");
        }
        if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
            throw ApiException.badRequest("An invoice must contain at least one line");
        }
        invoice.setCurrency(invoice.getCurrency().toUpperCase(Locale.ROOT));
        invoice.setStatus(InvoiceStatus.PENDING);
        if (invoice.getIssuedAt() == null) {
            invoice.setIssuedAt(Instant.now());
        }
        normaliseCoverage(invoice);
        recalculateAmounts(invoice);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice recalculate(UUID invoiceId) {
        Invoice invoice = get(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID) {
            throw ApiException.conflict("Invoice " + invoiceId + " cannot be recalculated in status "
                    + invoice.getStatus());
        }
        normaliseCoverage(invoice);
        recalculateAmounts(invoice);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice voidInvoice(UUID invoiceId) {
        Invoice invoice = get(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            return invoice;
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw ApiException.conflict("Paid invoice " + invoiceId + " cannot be voided");
        }
        invoice.setStatus(InvoiceStatus.VOID);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice applyPaymentCompleted(UUID invoiceId, UUID paymentId, Instant paidAt) {
        Invoice invoice = get(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID) {
            return invoice;
        }
        if (paymentId != null && paymentId.equals(invoice.getLastPaymentId())) {
            return invoice;
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(paidAt == null ? Instant.now() : paidAt);
        invoice.setFailureReason(null);
        invoice.setLastPaymentId(paymentId);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice applyPaymentFailed(UUID invoiceId, UUID paymentId, String reason) {
        Invoice invoice = get(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID) {
            return invoice;
        }
        if (paymentId != null && paymentId.equals(invoice.getLastPaymentId())) {
            return invoice;
        }
        invoice.setStatus(InvoiceStatus.FAILED);
        invoice.setFailureReason(reason == null || reason.isBlank() ? "Payment failed" : reason);
        invoice.setLastPaymentId(paymentId);
        invoice.setStatus(InvoiceStatus.PENDING);
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice get(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> ApiException.notFound("Invoice " + invoiceId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Invoice> list(UUID patientId, InvoiceStatus status) {
        if (patientId != null && status != null) {
            return invoiceRepository.findByPatientIdAndStatus(patientId, status);
        }
        if (patientId != null) {
            return invoiceRepository.findByPatientId(patientId);
        }
        if (status != null) {
            return invoiceRepository.findByStatus(status);
        }
        return invoiceRepository.findAll();
    }

    private void normaliseCoverage(Invoice invoice) {
        BigDecimal coverage = invoice.getInsuranceCoveragePercent();
        if (coverage == null) {
            coverage = BigDecimal.ZERO;
        }
        if (coverage.compareTo(BigDecimal.ZERO) < 0 || coverage.compareTo(HUNDRED) > 0) {
            throw ApiException.badRequest("insuranceCoveragePercent must be between 0 and 100");
        }
        invoice.setInsuranceCoveragePercent(coverage.setScale(2, RoundingMode.HALF_UP));
    }

    private void recalculateAmounts(Invoice invoice) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (InvoiceLine line : invoice.getLines()) {
            if (line.getQuantity() == null || line.getQuantity() <= 0) {
                throw ApiException.badRequest("Invoice line quantity must be greater than zero");
            }
            if (line.getUnitPrice() == null || line.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw ApiException.badRequest("Invoice line unitPrice must not be negative");
            }
            BigDecimal amount = line.getUnitPrice()
                    .multiply(BigDecimal.valueOf(line.getQuantity()))
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            line.setAmount(amount);
            subtotal = subtotal.add(amount);
        }
        subtotal = subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal insurerShare = subtotal
                .multiply(invoice.getInsuranceCoveragePercent())
                .divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal patientShare = subtotal.subtract(insurerShare).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        invoice.setSubtotal(subtotal);
        invoice.setInsurerShare(insurerShare);
        invoice.setPatientShare(patientShare);
        invoice.setTotalAmount(subtotal);
    }
}
