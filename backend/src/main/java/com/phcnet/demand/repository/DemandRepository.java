package com.phcnet.demand.repository;

import com.phcnet.demand.model.Demand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long> {

    @Query("SELECT d FROM Demand d JOIN FETCH d.phc JOIN FETCH d.medicine WHERE d.phc.id = :phcId AND d.medicine.id = :medicineId AND d.recordDate >= :since ORDER BY d.recordDate ASC")
    List<Demand> findHistory(@Param("phcId") Long phcId, @Param("medicineId") Long medicineId, @Param("since") LocalDate since);

    @Query("SELECT AVG(d.quantityUsed) FROM Demand d WHERE d.phc.id = :phcId AND d.medicine.id = :medicineId AND d.recordDate >= :since")
    Double getAverageDailyDemand(@Param("phcId") Long phcId, @Param("medicineId") Long medicineId, @Param("since") LocalDate since);

    @Query("SELECT d FROM Demand d JOIN FETCH d.phc JOIN FETCH d.medicine WHERE (:phcId IS NULL OR d.phc.id = :phcId) AND (:medicineId IS NULL OR d.medicine.id = :medicineId)")
    Page<Demand> findFiltered(@Param("phcId") Long phcId, @Param("medicineId") Long medicineId, Pageable pageable);
}
