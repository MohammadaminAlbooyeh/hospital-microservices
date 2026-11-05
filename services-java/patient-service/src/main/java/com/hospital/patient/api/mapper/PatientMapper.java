package com.hospital.patient.api.mapper;

import com.hospital.patient.api.dto.InsurancePolicyRequest;
import com.hospital.patient.api.dto.PatientRequest;
import com.hospital.patient.api.dto.PatientResponse;
import com.hospital.patient.domain.model.InsurancePolicy;
import com.hospital.patient.domain.model.Patient;

public final class PatientMapper {

    private PatientMapper() {
    }

    public static Patient toEntity(PatientRequest request) {
        return Patient.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .email(request.email())
                .phone(request.phone())
                .addressLine(request.addressLine())
                .city(request.city())
                .country(request.country())
                .nationalId(request.nationalId())
                .allergies(request.allergies())
                .emergencyContactName(request.emergencyContactName())
                .emergencyContactPhone(request.emergencyContactPhone())
                .insurancePolicy(toEntity(request.insurancePolicy()))
                .build();
    }

    public static InsurancePolicy toEntity(InsurancePolicyRequest request) {
        if (request == null) {
            return null;
        }
        return InsurancePolicy.builder()
                .provider(request.provider())
                .policyNumber(request.policyNumber())
                .memberId(request.memberId())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .coveragePercent(request.coveragePercent())
                .build();
    }

    public static PatientResponse toResponse(Patient patient) {
        PatientResponse.InsurancePolicyResponse insurance = null;
        if (patient.getInsurancePolicy() != null) {
            InsurancePolicy policy = patient.getInsurancePolicy();
            insurance = new PatientResponse.InsurancePolicyResponse(
                    policy.getId(), policy.getProvider(), policy.getPolicyNumber(),
                    policy.getMemberId(), policy.getValidFrom(), policy.getValidTo(),
                    policy.getCoveragePercent());
        }
        return new PatientResponse(
                patient.getId(),
                patient.getMrn(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getAddressLine(),
                patient.getCity(),
                patient.getCountry(),
                patient.getNationalId(),
                patient.getAllergies(),
                patient.getEmergencyContactName(),
                patient.getEmergencyContactPhone(),
                insurance,
                patient.getVersion(),
                patient.getCreatedAt(),
                patient.getUpdatedAt());
    }
}
