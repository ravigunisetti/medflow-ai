package com.phcnet.optimization.service;

import com.phcnet.optimization.dto.OptimizationRequest;
import com.phcnet.optimization.dto.RedistributionRecommendationDTO;
import com.phcnet.transfer.dto.TransferDTO;

import java.util.List;

public interface OptimizationService {
    List<RedistributionRecommendationDTO> calculateRedistributions(OptimizationRequest request);
    List<TransferDTO> applyRecommendations(List<RedistributionRecommendationDTO> recommendations);
}
