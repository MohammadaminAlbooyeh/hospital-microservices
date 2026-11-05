package com.hospital.encounter.domain.service;

import com.hospital.encounter.domain.model.Encounter;
import com.hospital.encounter.domain.model.EncounterState;
import com.hospital.encounter.domain.repository.EncounterRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EncounterDomainService {

    private final EncounterRepository encounterRepository;
    private final EncounterStateMachine stateMachine;

    @Transactional
    public Encounter create(UUID patientId, UUID doctorId, UUID departmentId, String chiefComplaint) {
        if (patientId == null) {
            throw ApiException.badRequest("patientId is required");
        }
        Encounter encounter = Encounter.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .departmentId(departmentId)
                .state(EncounterState.CREATED)
                .chiefComplaint(chiefComplaint)
                .build();
        return encounterRepository.save(encounter);
    }

    @Transactional(readOnly = true)
    public Encounter get(UUID id) {
        return encounterRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Encounter " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Encounter> search(UUID patientId, UUID doctorId, EncounterState state) {
        return encounterRepository.search(patientId, doctorId, state);
    }

    @Transactional
    public Encounter checkIn(UUID id) {
        return transition(id, EncounterState.CHECKED_IN);
    }

    @Transactional
    public Encounter start(UUID id) {
        return transition(id, EncounterState.IN_CONSULTATION);
    }

    @Transactional
    public Encounter requestLabs(UUID id) {
        return transition(id, EncounterState.LAB_PENDING);
    }

    @Transactional
    public Encounter requestPharmacy(UUID id) {
        return transition(id, EncounterState.PHARMACY_PENDING);
    }

    @Transactional
    public Encounter complete(UUID id, List<String> diagnoses) {
        if (diagnoses == null || diagnoses.isEmpty()) {
            throw ApiException.badRequest("At least one diagnosis is required to complete an encounter");
        }
        Encounter encounter = get(id);
        encounter.setDiagnoses(String.join(", ", diagnoses));
        return encounterRepository.save(stateMachine.transition(encounter, EncounterState.COMPLETED));
    }

    @Transactional
    public Encounter cancel(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ApiException.badRequest("A cancellation reason is required");
        }
        Encounter encounter = get(id);
        encounter.setCancelReason(reason);
        return encounterRepository.save(stateMachine.transition(encounter, EncounterState.CANCELLED));
    }

    private Encounter transition(UUID id, EncounterState target) {
        Encounter encounter = get(id);
        return encounterRepository.save(stateMachine.transition(encounter, target));
    }
}
