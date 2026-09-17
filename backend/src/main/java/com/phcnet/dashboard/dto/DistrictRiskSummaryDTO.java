package com.phcnet.dashboard.dto;

public record DistrictRiskSummaryDTO(
    String district,
    long totalPhcs,
    long criticalPhcs,
    long highRiskPhcs,
    double averageDaysRemaining,
    String overallStatus
) {}
