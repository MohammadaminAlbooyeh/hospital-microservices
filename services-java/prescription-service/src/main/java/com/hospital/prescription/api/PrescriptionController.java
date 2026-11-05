package com.hospital.prescription.api;

import com.hospital.prescription.api.dto.IssuePrescriptionRequest;
import com.hospital.prescription.api.dto.PrescriptionResponse;
import com.hospital.prescription.api.mapper.PrescriptionMapper;
import com.hospital.prescription.application.IssuePrescriptionResult;
import com.hospital.prescription.application.IssuePrescriptionUseCase;
import com.hospital.prescription.domain.model.Prescription;
import com.hospital.prescription.domain.service.PrescriptionDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final IssuePrescriptionUseCase issuePrescriptionUseCase;
    private final PrescriptionDomainService prescriptionDomainService;

    @PostMapping
    public ResponseEntity<PrescriptionResponse> issue(@Valid @RequestBody IssuePrescriptionRequest request) {
        IssuePrescriptionResult result = issuePrescriptionUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/prescriptions/" + result.prescription().getId()))
                .body(PrescriptionMapper.toResponse(result.prescription(), result.warnings()));
    }

    @GetMapping("/{id}")
    public PrescriptionResponse get(@PathVariable UUID id) {
        return PrescriptionMapper.toResponse(prescriptionDomainService.get(id));
    }

    @GetMapping
    public List<PrescriptionResponse> search(@RequestParam(required = false) UUID patientId,
                                             @RequestParam(required = false) UUID encounterId) {
        return prescriptionDomainService.search(patientId, encounterId).stream()
                .map(PrescriptionMapper::toResponse)
                .toList();
    }

    @PostMapping("/{id}/dispense")
    public PrescriptionResponse dispense(@PathVariable UUID id) {
        Prescription prescription = prescriptionDomainService.dispense(id);
        return PrescriptionMapper.toResponse(prescription);
    }

    @PostMapping("/{id}/cancel")
    public PrescriptionResponse cancel(@PathVariable UUID id) {
        Prescription prescription = prescriptionDomainService.cancel(id);
        return PrescriptionMapper.toResponse(prescription);
    }
}
