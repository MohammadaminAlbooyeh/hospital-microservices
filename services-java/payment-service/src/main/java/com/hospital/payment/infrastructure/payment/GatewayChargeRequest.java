package com.hospital.payment.infrastructure.payment;

import com.hospital.payment.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record GatewayChargeRequest(
        UUID paymentId,
        UUID invoiceId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        boolean simulateFailure
) {
}
