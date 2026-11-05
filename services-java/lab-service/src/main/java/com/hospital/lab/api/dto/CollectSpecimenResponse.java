package com.hospital.lab.api.dto;

public record CollectSpecimenResponse(
        LabOrderResponse order,
        SpecimenResponse specimen
) {
}
