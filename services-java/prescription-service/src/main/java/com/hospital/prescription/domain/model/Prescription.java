package com.hospital.prescription.domain.model;

import com.hospital.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prescription extends BaseEntity {

    @Column(nullable = false)
    private UUID patientId;

    private UUID encounterId;

    @Column(nullable = false)
    private UUID doctorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrescriptionStatus status;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private Instant issuedAt;

    @Builder.Default
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PrescriptionItem> items = new ArrayList<>();

    public void addItem(PrescriptionItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        item.setPrescription(this);
    }
}
