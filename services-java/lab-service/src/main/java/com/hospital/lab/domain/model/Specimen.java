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
@Table(name = "specimens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Specimen extends BaseEntity {

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false, unique = true, length = 128)
    private String barcode;

    @Column(nullable = false)
    private Instant collectedAt;

    @Column(nullable = false)
    private String collectedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SpecimenStatus status;
}
