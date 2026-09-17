package com.phcnet.prediction.dto;

import com.phcnet.prediction.model.Prediction;
import java.time.Instant;

public record PredictionDTO(
    Long id,
    Long phcId,
    String phcName,
    String district,
    Long medicineId,
    String medicineCode,
    String medicineName,
    Double predictedDailyDemand,
    Double predictedDaysToStockout,
    String riskLevel,
    String modelVersion,
    Double confidenceScore,
    Instant createdAt
) {
    public static PredictionDTO from(Prediction p) {
        return new PredictionDTO(
            p.getId(),
            p.getPhc().getId(),
            p.getPhc().getName(),
            p.getPhc().getDistrict(),
            p.getMedicine().getId(),
            p.getMedicine().getCode(),
            p.getMedicine().getName(),
            Math.round(p.getPredictedDailyDemand() * 100.0) / 100.0,
            Math.round(p.getPredictedDaysToStockout() * 10.0) / 10.0,
            p.getRiskLevel(),
            p.getModelVersion(),
            p.getConfidenceScore(),
            p.getCreatedAt()
        );
    }
}
