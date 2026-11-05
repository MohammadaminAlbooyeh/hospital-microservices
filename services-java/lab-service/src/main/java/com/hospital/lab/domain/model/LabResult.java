package com.hospital.lab.domain.model;

import com.hospital.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResult extends BaseEntity {

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
    private String value;

    @Column(length = 64)
    private String unit;

    private String referenceRange;

    @Column(nullable = false)
    private boolean critical;

    @Column(nullable = false)
    private Instant resultAt;

    @Column(columnDefinition = "text")
    private String notes;
}
