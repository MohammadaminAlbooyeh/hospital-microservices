package com.hospital.doctor.unit;

import com.hospital.doctor.domain.model.Doctor;
import com.hospital.doctor.domain.repository.DoctorLeaveRepository;
import com.hospital.doctor.domain.repository.DoctorRepository;
import com.hospital.doctor.domain.repository.DoctorScheduleRepository;
import com.hospital.doctor.domain.service.DoctorDomainService;
import com.hospital.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorDomainServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private DoctorLeaveRepository doctorLeaveRepository;

    @InjectMocks
    private DoctorDomainService doctorDomainService;

    @Test
    void registersADoctorWithDefaultCapacity() {
        when(doctorRepository.existsByEmail(any())).thenReturn(false);
        when(doctorRepository.existsByLicenseNumber(any())).thenReturn(false);
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Doctor saved = doctorDomainService.register(validDoctor());

        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getDailyCapacity()).isEqualTo(20);
    }

    @Test
    void rejectsDuplicateLicenseNumber() {
        when(doctorRepository.existsByEmail(any())).thenReturn(false);
        when(doctorRepository.existsByLicenseNumber("LIC-1")).thenReturn(true);

        assertThatThrownBy(() -> doctorDomainService.register(validDoctor()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
    }

    private Doctor validDoctor() {
        return Doctor.builder()
                .firstName("Greg")
                .lastName("House")
                .speciality("Diagnostics")
                .email("house@example.com")
                .phone("+100000000")
                .licenseNumber("LIC-1")
                .build();
    }
}
