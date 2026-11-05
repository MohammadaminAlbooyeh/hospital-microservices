package com.hospital.lab.api.dto;

import com.hospital.lab.domain.model.LabOrderStatus;
import com.hospital.lab.domain.model.LabPriority;

import java.time.Instant;
import java.util.UUID;

public record LabOrderResponse(
        UUID id,
        UUID patientId,
        UUID encounterId,
        String testCode,
        String testName,
        LabPriority priority,
        LabOrderStatus status,
        Instant orderedAt,
        String orderedBy,
        String notes,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
