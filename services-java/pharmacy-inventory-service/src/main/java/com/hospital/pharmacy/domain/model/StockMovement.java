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

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String drugCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(unique = true, length = 512)
    private String reference;
}
