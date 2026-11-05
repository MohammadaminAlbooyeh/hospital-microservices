package com.hospital.appointment.domain.model;

import com.hospital.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * An appointment slot held by a patient with a doctor. Concurrency safety
 * against double-booking is provided two ways: the inherited {@code
 * @Version} optimistic lock (guards reschedule/cancel races on the same
 * row) and a partial unique index on (doctor_id, slot_start) for active
 * appointments (guards two concurrent bookings of the same brand-new slot).
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment extends BaseEntity {

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID doctorId;

    private UUID departmentId;

    @Column(nullable = false)
    private Instant slotStart;

    private Instant slotEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Column(columnDefinition = "text")
    private String reason;

    private Instant cancelledAt;

    public boolean isActive() {
        return status != AppointmentStatus.CANCELLED;
    }
}
