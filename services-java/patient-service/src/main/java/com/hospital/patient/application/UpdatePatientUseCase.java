package com.hospital.patient.application;

import com.hospital.patient.api.dto.PatientRequest;
import com.hospital.patient.api.mapper.PatientMapper;
import com.hospital.patient.domain.model.Patient;
import com.hospital.patient.domain.service.PatientDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdatePatientUseCase {

    private final PatientDomainService patientDomainService;

    @Transactional
    public Patient execute(UUID id, PatientRequest request) {
        return patientDomainService.applyUpdate(id, PatientMapper.toEntity(request));
    }
}
