package com.hospital.pharmacy.domain.repository;

import com.hospital.pharmacy.domain.model.Drug;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DrugRepository {

    Drug save(Drug drug);

    Optional<Drug> findByCode(String code);

    List<Drug> findAllByOrderByNameAsc();

    @Query("select d from Drug d where d.quantityOnHand <= d.reorderLevel order by d.code")
    List<Drug> findLowStock();

    List<Drug> findByExpiryDateLessThanEqualOrderByExpiryDateAsc(LocalDate date);

    @Modifying(flushAutomatically = true)
    @Query("update Drug d set d.quantityOnHand = d.quantityOnHand - :quantity, d.version = d.version + 1 "
            + "where d.code = :code and d.quantityOnHand >= :quantity")
    int decrementStock(@Param("code") String code, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("update Drug d set d.quantityOnHand = d.quantityOnHand + :quantity, d.version = d.version + 1 "
            + "where d.code = :code")
    int incrementStock(@Param("code") String code, @Param("quantity") int quantity);
}
