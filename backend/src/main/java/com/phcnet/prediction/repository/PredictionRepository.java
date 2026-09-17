package com.phcnet.prediction.repository;

import com.phcnet.prediction.model.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    @Query("SELECT p FROM Prediction p JOIN FETCH p.phc JOIN FETCH p.medicine WHERE (:riskLevel IS NULL OR p.riskLevel = :riskLevel)")
    Page<Prediction> findByRiskLevel(@Param("riskLevel") String riskLevel, Pageable pageable);

    @Query("SELECT p FROM Prediction p JOIN FETCH p.phc JOIN FETCH p.medicine WHERE p.phc.id = :phcId")
    List<Prediction> findByPhcId(@Param("phcId") Long phcId);

    @Query("SELECT p FROM Prediction p JOIN FETCH p.phc JOIN FETCH p.medicine WHERE p.riskLevel IN ('CRITICAL', 'HIGH') ORDER BY p.predictedDaysToStockout ASC")
    List<Prediction> findCriticalAndHighAlerts();

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.riskLevel = 'CRITICAL'")
    long countCriticalPredictions();

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.riskLevel = 'HIGH'")
    long countHighPredictions();
}
