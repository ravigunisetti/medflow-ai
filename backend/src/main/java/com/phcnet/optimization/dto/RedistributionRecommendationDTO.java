package com.phcnet.optimization.dto;

public record RedistributionRecommendationDTO(
    Long sourcePhcId,
    String sourcePhcName,
    String sourceDistrict,
    Long destinationPhcId,
    String destinationPhcName,
    String destinationDistrict,
    Long medicineId,
    String medicineCode,
    String medicineName,
    String medicineUnit,
    int quantity,
    double distanceKm,
    double estimatedCost,
    String projectedRiskBefore,
    String projectedRiskAfter,
    double projectedDaysBefore,
    double projectedDaysAfter,
    String reason
) {}
