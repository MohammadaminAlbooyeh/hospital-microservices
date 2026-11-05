package com.hospital.lab.api.dto;

import java.time.Instant;
import java.util.UUID;

public record LabResultResponse(
        UUID id,
        UUID orderId,
        String value,
        String unit,
        String referenceRange,
        boolean critical,
        Instant resultAt,
        String notes
) {
}
