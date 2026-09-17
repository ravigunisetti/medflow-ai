package com.phcnet.prediction.dto;

import java.time.Instant;

public record RiskExplanationDTO(
    Long phcId,
    String phcName,
    String district,
    Long medicineId,
    String medicineName,
    int currentStock,
    int safetyStock,
    double averageDailyDemand,
    double daysToStockout,
    String riskLevel,
    double criticalThresholdDays,
    double highThresholdDays,
    double mediumThresholdDays,
    String formulaUsed,
    String mathematicalDerivation,
    String humanReadableExplanation,
    Instant timestamp
) {}
