package com.phcnet.analytics.dto;

import java.util.List;

public record AnalyticsReportDTO(
    List<DistrictRiskRankDTO> topStockoutDistricts,
    List<TopMedicineDemandDTO> topDemandedMedicines,
    List<OverstockPhcDTO> frequentOverstockPhcs,
    List<SeasonalTrendDTO> seasonalDemandAnalysis,
    RedistributionEfficiencyDTO redistributionMetrics
) {
    public record DistrictRiskRankDTO(String district, long criticalCount, long totalPhcs, double stockoutRatePct) {}
    public record TopMedicineDemandDTO(Long medicineId, String medicineName, String category, long totalUnitsDispensed) {}
    public record OverstockPhcDTO(Long phcId, String phcName, String district, int overstockedMedicinesCount, double avgBufferRatio) {}
    public record SeasonalTrendDTO(String medicineName, String category, String peakSeason, double surgeMultiplier) {}
    public record RedistributionEfficiencyDTO(
        long totalTransfersProposed,
        long stockoutsPrevented,
        double totalRedistributedUnits,
        double totalTransportationCostINR,
        double estimatedEmergencyReorderCostSavedINR,
        double costBenefitRatio
    ) {}
}
