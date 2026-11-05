package com.hospital.patient.api.dto;

import com.hospital.patient.domain.model.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String mrn,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String email,
        String phone,
        String addressLine,
        String city,
        String country,
        String nationalId,
        String allergies,
        String emergencyContactName,
        String emergencyContactPhone,
        InsurancePolicyResponse insurancePolicy,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public record InsurancePolicyResponse(
            UUID id,
            String provider,
            String policyNumber,
            String memberId,
            LocalDate validFrom,
            LocalDate validTo,
            Integer coveragePercent
    ) {
    }
}
