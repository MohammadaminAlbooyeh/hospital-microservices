package com.hospital.billing.api.dto;

import com.hospital.billing.domain.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID encounterId,
        UUID patientId,
        InvoiceStatus status,
        String currency,
        BigDecimal subtotal,
        BigDecimal insurerShare,
        BigDecimal patientShare,
        BigDecimal totalAmount,
        BigDecimal insuranceCoveragePercent,
        Instant issuedAt,
        Instant paidAt,
        String failureReason,
        UUID lastPaymentId,
        Instant createdAt,
        Instant updatedAt,
        List<InvoiceLineResponse> lines
) {
}
