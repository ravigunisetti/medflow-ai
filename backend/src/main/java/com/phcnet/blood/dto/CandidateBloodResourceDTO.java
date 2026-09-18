package com.phcnet.blood.dto;

import com.phcnet.blood.model.VerificationStatus;
import java.time.Instant;

public record CandidateBloodResourceDTO(
        Long bloodBankId,
        String bloodBankName,
        String district,
        Double latitude,
        Double longitude,
        VerificationStatus verificationStatus,
        String contactPhone,
        String bloodGroup,
        String componentType,
        Integer unitsAvailable,
        Integer unreservedUnits,
        Double distanceKm,
        Integer etaMinutes,
        Instant estimatedArrival,
        Boolean meetsDeadline,
        Double matchScore,
        Boolean isExactMatch,
        String matchReason
) {}
