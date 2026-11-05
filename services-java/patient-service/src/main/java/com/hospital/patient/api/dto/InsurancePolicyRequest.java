package com.hospital.patient.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record InsurancePolicyRequest(
        @NotBlank String provider,
        @NotBlank String policyNumber,
        @NotBlank String memberId,
        LocalDate validFrom,
        LocalDate validTo,
        @Min(0) @Max(100) Integer coveragePercent
) {
}
