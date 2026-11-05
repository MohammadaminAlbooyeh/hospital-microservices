package com.hospital.patient.api.dto;

import com.hospital.patient.domain.model.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank @Email String email,
        @NotBlank String phone,
        String addressLine,
        String city,
        String country,
        @NotBlank String nationalId,
        String allergies,
        String emergencyContactName,
        String emergencyContactPhone,
        @Valid InsurancePolicyRequest insurancePolicy
) {
}
