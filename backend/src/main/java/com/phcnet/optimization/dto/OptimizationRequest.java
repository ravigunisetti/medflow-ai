package com.phcnet.optimization.dto;

public record OptimizationRequest(
    String district,
    Long medicineId,
    Double maxDistanceKm,
    Double targetStockDays
) {}
