package com.phcnet.simulation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SimulationRequestDTO(
        String scenarioId,
        String district,
        @Min(0) @Max(500) Integer footfallSurgePercent,
        @Min(0) @Max(500) Integer acuteDemandMultiplier,
        @Min(0) @Max(60) Integer supplyDelayDays,
        String affectedCategory
) {
    public int getFootfallSurgePercent() {
        return footfallSurgePercent != null ? footfallSurgePercent : 50;
    }

    public int getAcuteDemandMultiplier() {
        return acuteDemandMultiplier != null ? acuteDemandMultiplier : 100;
    }

    public int getSupplyDelayDays() {
        return supplyDelayDays != null ? supplyDelayDays : 14;
    }

    public String getAffectedCategory() {
        return (affectedCategory != null && !affectedCategory.isBlank()) ? affectedCategory : "Analgesics & Antipyretics";
    }
}
