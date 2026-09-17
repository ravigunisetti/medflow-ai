package com.phcnet.inventory.repository;

import com.phcnet.inventory.model.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query("SELECT i FROM Inventory i JOIN FETCH i.phc JOIN FETCH i.medicine WHERE (:phcId IS NULL OR i.phc.id = :phcId) AND (:medicineId IS NULL OR i.medicine.id = :medicineId)")
    Page<Inventory> findFiltered(@Param("phcId") Long phcId, @Param("medicineId") Long medicineId, Pageable pageable);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.phc JOIN FETCH i.medicine WHERE i.phc.id = :phcId")
    List<Inventory> findByPhcId(@Param("phcId") Long phcId);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.phc JOIN FETCH i.medicine WHERE i.phc.id = :phcId AND i.medicine.id = :medicineId")
    Optional<Inventory> findByPhcIdAndMedicineId(@Param("phcId") Long phcId, @Param("medicineId") Long medicineId);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.phc JOIN FETCH i.medicine WHERE i.medicine.id = :medicineId AND i.quantity > (i.medicine.safetyStock * 1.5)")
    List<Inventory> findSurplusForMedicine(@Param("medicineId") Long medicineId);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantity < i.medicine.safetyStock")
    long countCriticalStockouts();
}
