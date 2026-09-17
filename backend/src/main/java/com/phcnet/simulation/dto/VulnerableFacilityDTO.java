package com.phcnet.simulation.dto;

public record VulnerableFacilityDTO(
        Long phcId,
        String phc,
        String district,
        String medicineName,
        String medicineCode,
        int currentStock,
        double baselineDemand,
        double crisisDemand,
        double beforeStockDays,
        double afterStockDays,
        String newRisk
) {}
