package com.hospital.pharmacy.infrastructure.persistence;

import com.hospital.pharmacy.domain.model.StockMovement;
import com.hospital.pharmacy.domain.repository.StockMovementRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaStockMovementRepository extends JpaRepository<StockMovement, UUID>, StockMovementRepository {
}
