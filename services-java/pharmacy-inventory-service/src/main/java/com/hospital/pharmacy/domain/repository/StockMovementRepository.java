package com.hospital.pharmacy.domain.repository;

import com.hospital.pharmacy.domain.model.StockMovement;

public interface StockMovementRepository {

    StockMovement save(StockMovement movement);

    boolean existsByReference(String reference);
}
