package com.hospital.lab.api.dto;

import com.hospital.lab.domain.model.SpecimenStatus;

import java.time.Instant;
import java.util.UUID;

public record SpecimenResponse(
        UUID id,
        UUID orderId,
        String barcode,
        Instant collectedAt,
        String collectedBy,
        SpecimenStatus status
) {
}
