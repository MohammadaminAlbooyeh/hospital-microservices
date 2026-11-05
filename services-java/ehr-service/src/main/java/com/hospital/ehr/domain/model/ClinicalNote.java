package com.hospital.ehr.domain.model;

import com.hospital.shared.persistence.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clinical_notes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_clinical_notes_patient_version",
                columnNames = {"patient_id", "version"}))
@AttributeOverride(name = "version", column = @Column(name = "entity_version", nullable = false))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClinicalNote extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "encounter_id")
    private UUID encounterId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "version", nullable = false)
    private int clinicalVersion;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "text")
    private String diagnoses;

    @Column(name = "supersedes_id")
    private UUID supersedesId;

    public List<String> diagnosisList() {
        if (diagnoses == null || diagnoses.isBlank()) {
            return List.of();
        }
        return Arrays.stream(diagnoses.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .toList();
    }
}
