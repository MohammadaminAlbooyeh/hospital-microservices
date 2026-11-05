package com.hospital.lab.infrastructure.messaging;

import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.model.LabResult;
import com.hospital.shared.events.EventTypes;
import com.hospital.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LabEventPublisher {

    private final OutboxService outboxService;

    public void labResultReady(LabOrder order, LabResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("labOrderId", order.getId().toString());
        payload.put("patientId", order.getPatientId().toString());
        payload.put("testCode", order.getTestCode());
        payload.put("resultSummary", summarise(order, result));
        payload.put("critical", result.isCritical());
        payload.put("readyAt", result.getResultAt().toString());
        outboxService.publish("LabOrder", order.getId().toString(), EventTypes.LAB_RESULT_READY, payload);
    }

    private String summarise(LabOrder order, LabResult result) {
        StringBuilder summary = new StringBuilder(order.getTestName())
                .append(": ")
                .append(result.getValue());
        if (result.getUnit() != null && !result.getUnit().isBlank()) {
            summary.append(' ').append(result.getUnit());
        }
        if (result.getReferenceRange() != null && !result.getReferenceRange().isBlank()) {
            summary.append(" (ref ").append(result.getReferenceRange()).append(')');
        }
        if (result.isCritical()) {
            summary.append(" [CRITICAL]");
        }
        return summary.toString();
    }
}
