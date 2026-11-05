package com.hospital.payment.api.dto;

import com.hospital.payment.domain.model.PaymentMethod;
import com.hospital.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID invoiceId,
        UUID patientId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        String idempotencyKey,
        String gatewayReference,
        String failureReason,
        Instant processedAt,
        Instant refundedAt,
        BigDecimal refundAmount,
        Instant createdAt,
        Instant updatedAt
) {
}
