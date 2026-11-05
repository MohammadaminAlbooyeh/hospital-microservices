package com.hospital.encounter.domain.model;

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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "encounters")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Encounter extends BaseEntity {

    @Column(nullable = false)
    private UUID patientId;

    private UUID doctorId;

    private UUID departmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EncounterState state;

    @Column(columnDefinition = "text")
    private String chiefComplaint;

    private Instant checkedInAt;

    private Instant startedAt;

    private Instant completedAt;

    private Instant cancelledAt;

    @Column(columnDefinition = "text")
    private String cancelReason;

    @Column(columnDefinition = "text")
    private String diagnoses;

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
