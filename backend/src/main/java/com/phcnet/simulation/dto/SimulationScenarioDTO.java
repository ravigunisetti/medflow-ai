package com.phcnet.simulation.dto;

public record SimulationScenarioDTO(
        String scenarioId,
        String name,
        String description,
        int defaultSurgePercent,
        int defaultAcuteMultiplier,
        int defaultSupplyDelayDays,
        String affectedCategory,
        String riskSeverity
) {}
