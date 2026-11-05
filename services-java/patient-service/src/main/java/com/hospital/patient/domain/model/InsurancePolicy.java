package com.hospital.patient.domain.model;

import com.hospital.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "insurance_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsurancePolicy extends BaseEntity {

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false, unique = true)
    private String policyNumber;

    @Column(nullable = false)
    private String memberId;

    private LocalDate validFrom;

    private LocalDate validTo;

    private Integer coveragePercent;
}
