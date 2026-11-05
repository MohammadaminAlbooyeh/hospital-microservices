package com.hospital.lab.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CollectSpecimenRequest(@NotBlank String collectedBy) {
}
