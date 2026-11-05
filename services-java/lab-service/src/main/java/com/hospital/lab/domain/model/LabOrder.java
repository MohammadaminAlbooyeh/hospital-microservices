package com.hospital.lab.domain.model;

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

@Entity
@Table(name = "lab_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabOrder extends BaseEntity {

    @Column(nullable = false)
    private UUID patientId;

    private UUID encounterId;

    @Column(nullable = false, length = 64)
    private String testCode;

    @Column(nullable = false)
    private String testName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LabPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LabOrderStatus status;

    @Column(nullable = false)
    private Instant orderedAt;

    @Column(nullable = false)
    private String orderedBy;

    @Column(columnDefinition = "text")
    private String notes;
}
