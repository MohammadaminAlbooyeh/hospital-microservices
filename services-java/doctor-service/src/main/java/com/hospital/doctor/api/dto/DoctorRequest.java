package com.hospital.doctor.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record DoctorRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String speciality,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank String licenseNumber,
        UUID departmentId,
        Integer dailyCapacity
) {
}
