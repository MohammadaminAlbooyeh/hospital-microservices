package com.hospital.patient.api;

import com.hospital.patient.api.dto.PatientRequest;
import com.hospital.patient.api.dto.PatientResponse;
import com.hospital.patient.api.mapper.PatientMapper;
import com.hospital.patient.application.RegisterPatientUseCase;
import com.hospital.patient.application.UpdatePatientUseCase;
import com.hospital.patient.domain.model.Patient;
import com.hospital.patient.domain.service.PatientDomainService;
import com.hospital.patient.infrastructure.client.InsuranceClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final RegisterPatientUseCase registerPatientUseCase;
    private final UpdatePatientUseCase updatePatientUseCase;
    private final PatientDomainService patientDomainService;
    private final InsuranceClient insuranceClient;

    @PostMapping
    public ResponseEntity<PatientResponse> register(@Valid @RequestBody PatientRequest request) {
        Patient patient = registerPatientUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/patients/" + patient.getId()))
                .body(PatientMapper.toResponse(patient));
    }

    @GetMapping("/{id}")
    public PatientResponse get(@PathVariable UUID id) {
        return PatientMapper.toResponse(patientDomainService.get(id));
    }

    @GetMapping("/by-email/{email}")
    public PatientResponse getByEmail(@PathVariable String email) {
        return PatientMapper.toResponse(patientDomainService.getByEmail(email));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR','NURSE','BILLING')")
    public List<PatientResponse> list() {
        return patientDomainService.list().stream().map(PatientMapper::toResponse).toList();
    }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable UUID id, @Valid @RequestBody PatientRequest request) {
        return PatientMapper.toResponse(updatePatientUseCase.execute(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        patientDomainService.delete(id);
    }

    @GetMapping("/{id}/insurance-eligibility")
    public Map<String, Object> insuranceEligibility(@PathVariable UUID id) {
        Patient patient = patientDomainService.get(id);
        if (patient.getInsurancePolicy() == null) {
            return Map.of("eligible", false, "reason", "no insurance policy on file");
        }
        return insuranceClient.checkEligibility(
                patient.getInsurancePolicy().getPolicyNumber(),
                patient.getInsurancePolicy().getMemberId());
    }
}
