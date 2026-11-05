package com.hospital.encounter.domain.repository;

import com.hospital.encounter.domain.model.Encounter;
import com.hospital.encounter.domain.model.EncounterState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EncounterRepository {

    Encounter save(Encounter encounter);

    Optional<Encounter> findById(UUID id);

    List<Encounter> search(UUID patientId, UUID doctorId, EncounterState state);
}
