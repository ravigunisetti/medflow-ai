package com.phcnet.blood.dto;

import java.util.List;

public record BloodMatchingResultDTO(
        EmergencyBloodRequestDTO request,
        List<String> compatibleGroups,
        List<CandidateBloodResourceDTO> candidates,
        CandidateBloodResourceDTO recommendedSource,
        String aiExplanation,
        String safetyDisclaimer
) {}
