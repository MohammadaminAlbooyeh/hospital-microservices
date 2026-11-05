package com.hospital.patient.unit;

import com.hospital.patient.domain.model.Gender;
import com.hospital.patient.domain.model.Patient;
import com.hospital.patient.domain.repository.PatientRepository;
import com.hospital.patient.domain.service.PatientDomainService;
import com.hospital.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientDomainServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientDomainService patientDomainService;

    @Test
    void assignsAnMrnWhenRegistering() {
        when(patientRepository.existsByEmail(any())).thenReturn(false);
        when(patientRepository.existsByNationalId(any())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Patient saved = patientDomainService.register(validPatient());

        assertThat(saved.getMrn()).startsWith("MRN-");
    }

    @Test
    void rejectsDuplicateEmail() {
        when(patientRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> patientDomainService.register(validPatient()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
    }

    private Patient validPatient() {
        return Patient.builder()
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.FEMALE)
                .email("jane@example.com")
                .phone("+100000000")
                .nationalId("NAT-1")
                .build();
    }
}
