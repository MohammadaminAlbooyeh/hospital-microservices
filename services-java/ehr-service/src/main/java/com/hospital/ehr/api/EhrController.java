package com.hospital.ehr.api;

import com.hospital.ehr.api.dto.ClinicalNoteResponse;
import com.hospital.ehr.api.dto.CreateClinicalNoteRequest;
import com.hospital.ehr.api.mapper.ClinicalNoteMapper;
import com.hospital.ehr.application.CreateClinicalNoteUseCase;
import com.hospital.ehr.domain.model.ClinicalNote;
import com.hospital.ehr.domain.service.ClinicalNoteDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ehr")
@RequiredArgsConstructor
public class EhrController {

    private final CreateClinicalNoteUseCase createClinicalNoteUseCase;
    private final ClinicalNoteDomainService clinicalNoteDomainService;

    @PostMapping("/notes")
    public ResponseEntity<ClinicalNoteResponse> create(@Valid @RequestBody CreateClinicalNoteRequest request) {
        ClinicalNote note = createClinicalNoteUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/ehr/notes/" + note.getId()))
                .body(ClinicalNoteMapper.toResponse(note));
    }

    @GetMapping("/patients/{patientId}/current")
    public ClinicalNoteResponse current(@PathVariable UUID patientId) {
        return ClinicalNoteMapper.toResponse(clinicalNoteDomainService.current(patientId));
    }

    @GetMapping("/patients/{patientId}/history")
    public List<ClinicalNoteResponse> history(@PathVariable UUID patientId) {
        return clinicalNoteDomainService.history(patientId).stream()
                .map(ClinicalNoteMapper::toResponse)
                .toList();
    }

    @GetMapping("/patients/{patientId}/versions/{version}")
    public ClinicalNoteResponse version(@PathVariable UUID patientId, @PathVariable int version) {
        return ClinicalNoteMapper.toResponse(clinicalNoteDomainService.getVersion(patientId, version));
    }

    @GetMapping("/notes/{id}")
    public ClinicalNoteResponse get(@PathVariable UUID id) {
        return ClinicalNoteMapper.toResponse(clinicalNoteDomainService.get(id));
    }

    @GetMapping("/patients/{patientId}/diagnoses")
    public List<String> diagnoses(@PathVariable UUID patientId) {
        return clinicalNoteDomainService.diagnoses(patientId);
    }
}
