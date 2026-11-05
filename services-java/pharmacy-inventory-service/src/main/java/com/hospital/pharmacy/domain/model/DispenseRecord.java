package com.hospital.pharmacy.domain.model;

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
@Table(name = "dispense_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispenseRecord extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(nullable = false)
    private UUID patientId;

    private UUID encounterId;

    private UUID prescriptionId;

    @Column(nullable = false, length = 64)
    private String drugCode;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DispenseStatus status;

    private Instant finalizedAt;
}
