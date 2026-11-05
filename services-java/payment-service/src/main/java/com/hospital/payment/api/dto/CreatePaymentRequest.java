package com.hospital.payment.api.dto;

import com.hospital.payment.domain.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID invoiceId,
        UUID patientId,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        @NotNull PaymentMethod method,
        Boolean simulateFailure
) {
}
