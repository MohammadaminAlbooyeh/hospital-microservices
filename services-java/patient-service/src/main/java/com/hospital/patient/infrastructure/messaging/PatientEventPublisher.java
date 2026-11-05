package com.hospital.patient.infrastructure.messaging;

import com.hospital.patient.domain.model.Patient;
import com.hospital.shared.events.EventTypes;
import com.hospital.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PatientEventPublisher {

    private final OutboxService outboxService;

    public void patientRegistered(Patient patient) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("patientId", patient.getId().toString());
        payload.put("mrn", patient.getMrn());
        payload.put("firstName", patient.getFirstName());
        payload.put("lastName", patient.getLastName());
        payload.put("email", patient.getEmail());
        payload.put("phone", patient.getPhone());
        payload.put("registeredAt", Instant.now().toString());
        if (patient.getInsurancePolicy() != null) {
            payload.put("insuranceProvider", patient.getInsurancePolicy().getProvider());
            payload.put("insurancePolicyNumber", patient.getInsurancePolicy().getPolicyNumber());
        }
        outboxService.publish("Patient", patient.getId().toString(), EventTypes.PATIENT_REGISTERED, payload);
    }
}
