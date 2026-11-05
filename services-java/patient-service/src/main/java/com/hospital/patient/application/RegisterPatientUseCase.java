package com.hospital.patient.application;

import com.hospital.patient.api.dto.PatientRequest;
import com.hospital.patient.api.mapper.PatientMapper;
import com.hospital.patient.domain.model.Patient;
import com.hospital.patient.domain.service.PatientDomainService;
import com.hospital.patient.infrastructure.messaging.PatientEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterPatientUseCase {

    private final PatientDomainService patientDomainService;
    private final PatientEventPublisher eventPublisher;

    @Transactional
    public Patient execute(PatientRequest request) {
        Patient patient = patientDomainService.register(PatientMapper.toEntity(request));
        eventPublisher.patientRegistered(patient);
        return patient;
    }
}
