package com.hospital.payment.domain.service;

import com.hospital.payment.domain.model.Payment;
import com.hospital.payment.domain.model.PaymentStatus;
import com.hospital.payment.domain.repository.PaymentRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentDomainService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Payment get(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Payment " + paymentId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Payment> list(UUID invoiceId) {
        if (invoiceId == null) {
            return paymentRepository.findAll();
        }
        return paymentRepository.findByInvoiceId(invoiceId);
    }

    @Transactional
    public Payment refund(UUID paymentId, BigDecimal requestedAmount) {
        Payment payment = get(paymentId);
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw ApiException.conflict("Only successful payments can be refunded; payment "
                    + paymentId + " is " + payment.getStatus());
        }
        BigDecimal refund = requestedAmount == null ? payment.getAmount() : requestedAmount;
        if (refund.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Refund amount must be greater than zero");
        }
        BigDecimal alreadyRefunded = payment.getRefundAmount() == null ? ZERO : payment.getRefundAmount();
        BigDecimal totalRefunded = alreadyRefunded.add(refund).setScale(2, RoundingMode.HALF_UP);
        if (totalRefunded.compareTo(payment.getAmount()) > 0) {
            throw ApiException.badRequest("Refund amount exceeds the payment amount");
        }
        payment.setRefundAmount(totalRefunded);
        if (totalRefunded.compareTo(payment.getAmount()) == 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(Instant.now());
        }
        return paymentRepository.save(payment);
    }
}
