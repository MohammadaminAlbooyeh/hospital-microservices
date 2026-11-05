package com.hospital.encounter.infrastructure.messaging;

import com.hospital.encounter.domain.model.Encounter;
import com.hospital.shared.events.EventTypes;
import com.hospital.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EncounterEventPublisher {

    private final OutboxService outboxService;

    public void encounterCompleted(Encounter encounter) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("encounterId", encounter.getId().toString());
        payload.put("patientId", encounter.getPatientId().toString());
        payload.put("doctorId", encounter.getDoctorId() == null ? null : encounter.getDoctorId().toString());
        payload.put("diagnoses", encounter.diagnosisList());
        payload.put("completedAt", encounter.getCompletedAt() == null
                ? Instant.now().toString()
                : encounter.getCompletedAt().toString());
        outboxService.publish("Encounter", encounter.getId().toString(),
                EventTypes.ENCOUNTER_COMPLETED, payload);
    }
}
