package com.hospital.pharmacy.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockUpsertRequest(
        @NotBlank String code,
        @NotBlank String name,
        String form,
        String unit,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitPrice,
        @Min(0) int quantity,
        @Min(0) int reorderLevel,
        LocalDate expiryDate
) {
}
