package com.phcnet.blood.dto;

import java.util.Map;

public record BloodDashboardSummaryDTO(
        long totalBloodBanks,
        long activeBloodBanks,
        long verifiedBloodBanks,
        long totalUnitsAvailable,
        long totalUnitsReserved,
        long totalEmergencyRequests,
        long activeEmergencies,
        long fulfilledEmergencies,
        long activeTransfers,
        Map<String, Long> unitsByBloodGroup,
        Map<String, Long> requestsByStatus
) {}
