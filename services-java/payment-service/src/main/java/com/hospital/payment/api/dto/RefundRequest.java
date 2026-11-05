package com.hospital.payment.api.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record RefundRequest(
        @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal amount
) {
}
