package com.hospital.encounter.application;

import com.hospital.encounter.domain.model.Encounter;
import com.hospital.encounter.domain.service.EncounterDomainService;
import com.hospital.encounter.infrastructure.messaging.EncounterEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompleteEncounterUseCase {

    private final EncounterDomainService encounterDomainService;
    private final EncounterEventPublisher eventPublisher;

    @Transactional
    public Encounter execute(UUID encounterId, List<String> diagnoses) {
        Encounter encounter = encounterDomainService.complete(encounterId, diagnoses);
        eventPublisher.encounterCompleted(encounter);
        return encounter;
    }
}
