package com.hospital.payment.application;

import com.hospital.payment.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargePaymentCommand(
        UUID invoiceId,
        UUID patientId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        boolean simulateFailure,
        String idempotencyKey
) {
}
