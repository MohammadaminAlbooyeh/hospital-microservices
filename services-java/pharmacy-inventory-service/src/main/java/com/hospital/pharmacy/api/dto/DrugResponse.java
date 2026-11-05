package com.hospital.pharmacy.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DrugResponse(
        UUID id,
        String code,
        String name,
        String form,
        String unit,
        BigDecimal unitPrice,
        int quantityOnHand,
        int reorderLevel,
        LocalDate expiryDate,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
