package com.phcnet.dashboard.dto;

import java.util.List;

public record DashboardSummaryDTO(
    long totalPhcsMonitored,
    long totalMedicinesTracked,
    long criticalStockouts,
    long highRiskPredictions,
    long pendingTransfers,
    long completedTransfers,
    double networkHealthScore,
    List<DistrictRiskSummaryDTO> districtSummaries
) {}
