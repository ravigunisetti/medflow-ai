package com.phcnet.prediction.service;

import com.phcnet.prediction.dto.AlertDTO;
import com.phcnet.prediction.dto.PredictionDTO;
import com.phcnet.prediction.dto.RiskExplanationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PredictionService {
    Page<PredictionDTO> getPredictions(String riskLevel, Pageable pageable);
    List<AlertDTO> getActiveAlerts();
    PredictionDTO calculateAndSaveRisk(Long phcId, Long medicineId);
    RiskExplanationDTO explainRisk(Long phcId, Long medicineId);
    void evaluateAllFacilities();
}
