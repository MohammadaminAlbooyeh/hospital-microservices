package com.hospital.billing.api.dto;

import com.hospital.billing.domain.model.InvoiceLineCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineResponse(
        UUID id,
        InvoiceLineCategory category,
        String description,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal amount
) {
}
