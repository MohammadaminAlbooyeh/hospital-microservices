package com.hospital.patient.domain.service;

import com.hospital.patient.domain.model.Patient;
import com.hospital.patient.domain.repository.PatientRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientDomainService {

    private final PatientRepository patientRepository;

    @Transactional
    public Patient register(Patient patient) {
        if (patientRepository.existsByEmail(patient.getEmail())) {
            throw ApiException.conflict("A patient with email " + patient.getEmail() + " already exists");
        }
        if (patientRepository.existsByNationalId(patient.getNationalId())) {
            throw ApiException.conflict("A patient with national id already exists");
        }
        if (patient.getMrn() == null || patient.getMrn().isBlank()) {
            patient.setMrn(newMrn());
        }
        return patientRepository.save(patient);
    }

    @Transactional
    public Patient applyUpdate(UUID id, Patient patch) {
        Patient existing = get(id);
        if (!existing.getEmail().equals(patch.getEmail()) && patientRepository.existsByEmail(patch.getEmail())) {
            throw ApiException.conflict("A patient with email " + patch.getEmail() + " already exists");
        }
        if (!existing.getNationalId().equals(patch.getNationalId())
                && patientRepository.existsByNationalId(patch.getNationalId())) {
            throw ApiException.conflict("A patient with national id already exists");
        }
        existing.setFirstName(patch.getFirstName());
        existing.setLastName(patch.getLastName());
        existing.setDateOfBirth(patch.getDateOfBirth());
        existing.setGender(patch.getGender());
        existing.setEmail(patch.getEmail());
        existing.setPhone(patch.getPhone());
        existing.setAddressLine(patch.getAddressLine());
        existing.setCity(patch.getCity());
        existing.setCountry(patch.getCountry());
        existing.setNationalId(patch.getNationalId());
        existing.setAllergies(patch.getAllergies());
        existing.setEmergencyContactName(patch.getEmergencyContactName());
        existing.setEmergencyContactPhone(patch.getEmergencyContactPhone());
        if (patch.getInsurancePolicy() != null) {
            existing.setInsurancePolicy(patch.getInsurancePolicy());
        }
        return patientRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Patient get(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Patient " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Patient getByEmail(String email) {
        return patientRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Patient with email " + email + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Patient> list() {
        return patientRepository.findAll();
    }

    @Transactional
    public void delete(UUID id) {
        patientRepository.delete(get(id));
    }

    private String newMrn() {
        return "MRN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
