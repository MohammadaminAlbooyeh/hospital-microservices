package com.hospital.appointment.infrastructure.messaging;

import com.hospital.appointment.domain.model.Appointment;
import com.hospital.shared.events.EventTypes;
import com.hospital.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes AppointmentBooked / AppointmentCancelled events via the
 * transactional outbox. Field names mirror contracts/avro/AppointmentBooked.avsc
 * and AppointmentCancelled.avsc.
 */
@Component
@RequiredArgsConstructor
public class AppointmentEventPublisher {

    private final OutboxService outboxService;

    public void appointmentBooked(Appointment appointment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", appointment.getId().toString());
        payload.put("patientId", appointment.getPatientId().toString());
        payload.put("doctorId", appointment.getDoctorId().toString());
        payload.put("departmentId", appointment.getDepartmentId() == null ? null
                : appointment.getDepartmentId().toString());
        payload.put("slotStart", appointment.getSlotStart().toEpochMilli());
        payload.put("slotEnd", appointment.getSlotEnd() == null ? null : appointment.getSlotEnd().toEpochMilli());
        payload.put("bookedAt", Instant.now().toEpochMilli());
        outboxService.publish("Appointment", appointment.getId().toString(),
                EventTypes.APPOINTMENT_BOOKED, payload);
    }

    public void appointmentCancelled(Appointment appointment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", appointment.getId().toString());
        payload.put("patientId", appointment.getPatientId().toString());
        payload.put("reason", appointment.getReason());
        payload.put("cancelledAt", (appointment.getCancelledAt() == null ? Instant.now()
                : appointment.getCancelledAt()).toEpochMilli());
        outboxService.publish("Appointment", appointment.getId().toString(),
                EventTypes.APPOINTMENT_CANCELLED, payload);
    }
}
