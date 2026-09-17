package com.phcnet.prediction.dto;

import java.time.Instant;

public record AlertDTO(
    Long phcId,
    String phcName,
    String district,
    Long medicineId,
    String medicineName,
    Integer currentStock,
    Double dailyDemand,
    Double daysRemaining,
    String riskLevel,
    String message,
    Instant timestamp
) {}
