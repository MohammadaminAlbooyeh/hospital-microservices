package com.hospital.doctor.application;

import com.hospital.doctor.api.dto.DoctorRequest;
import com.hospital.doctor.api.mapper.DoctorMapper;
import com.hospital.doctor.domain.model.Doctor;
import com.hospital.doctor.domain.service.DoctorDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterDoctorUseCase {

    private final DoctorDomainService doctorDomainService;

    @Transactional
    public Doctor execute(DoctorRequest request) {
        return doctorDomainService.register(DoctorMapper.toEntity(request));
    }
}
