package com.hospital.billing.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotNull UUID patientId,
        UUID encounterId,
        @NotBlank String currency,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal insuranceCoveragePercent,
        @NotEmpty @Valid List<InvoiceLineRequest> lines
) {
}
