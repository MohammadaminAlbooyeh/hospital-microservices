package com.hospital.encounter.api;

import com.hospital.encounter.api.dto.CancelEncounterRequest;
import com.hospital.encounter.api.dto.CompleteEncounterRequest;
import com.hospital.encounter.api.dto.CreateEncounterRequest;
import com.hospital.encounter.api.dto.EncounterResponse;
import com.hospital.encounter.api.mapper.EncounterMapper;
import com.hospital.encounter.application.CompleteEncounterUseCase;
import com.hospital.encounter.domain.model.Encounter;
import com.hospital.encounter.domain.model.EncounterState;
import com.hospital.encounter.domain.service.EncounterDomainService;
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
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterDomainService encounterDomainService;
    private final CompleteEncounterUseCase completeEncounterUseCase;

    @PostMapping
    public ResponseEntity<EncounterResponse> create(@Valid @RequestBody CreateEncounterRequest request) {
        Encounter encounter = encounterDomainService.create(
                request.patientId(), request.doctorId(), request.departmentId(), request.chiefComplaint());
        return ResponseEntity.created(URI.create("/api/v1/encounters/" + encounter.getId()))
                .body(EncounterMapper.toResponse(encounter));
    }

    @GetMapping("/{id}")
    public EncounterResponse get(@PathVariable UUID id) {
        return EncounterMapper.toResponse(encounterDomainService.get(id));
    }

    @GetMapping
    public List<EncounterResponse> search(@RequestParam(required = false) UUID patientId,
                                          @RequestParam(required = false) UUID doctorId,
                                          @RequestParam(required = false) EncounterState state) {
        return encounterDomainService.search(patientId, doctorId, state).stream()
                .map(EncounterMapper::toResponse)
                .toList();
    }

    @PostMapping("/{id}/check-in")
    public EncounterResponse checkIn(@PathVariable UUID id) {
        return EncounterMapper.toResponse(encounterDomainService.checkIn(id));
    }

    @PostMapping("/{id}/start")
    public EncounterResponse start(@PathVariable UUID id) {
        return EncounterMapper.toResponse(encounterDomainService.start(id));
    }

    @PostMapping("/{id}/labs")
    public EncounterResponse labs(@PathVariable UUID id) {
        return EncounterMapper.toResponse(encounterDomainService.requestLabs(id));
    }

    @PostMapping("/{id}/pharmacy")
    public EncounterResponse pharmacy(@PathVariable UUID id) {
        return EncounterMapper.toResponse(encounterDomainService.requestPharmacy(id));
    }

    @PostMapping("/{id}/complete")
    public EncounterResponse complete(@PathVariable UUID id,
                                      @Valid @RequestBody CompleteEncounterRequest request) {
        return EncounterMapper.toResponse(completeEncounterUseCase.execute(id, request.diagnoses()));
    }

    @PostMapping("/{id}/cancel")
    public EncounterResponse cancel(@PathVariable UUID id,
                                    @Valid @RequestBody CancelEncounterRequest request) {
        return EncounterMapper.toResponse(encounterDomainService.cancel(id, request.reason()));
    }
}
