package com.phcnet.simulation.dto;

import com.phcnet.optimization.dto.RedistributionRecommendationDTO;
import java.util.List;

public record SimulationResultDTO(
        int beforeCriticalCount,
        int afterCriticalCount,
        int emergencyTransfersGenerated,
        int preventedStockouts,
        int affectedFacilitiesCount,
        double mitigationRatePercent,
        long executionTimeMs,
        String scenarioSummary,
        List<VulnerableFacilityDTO> vulnerableFacilities,
        List<RedistributionRecommendationDTO> recommendedTransfers
) {}
