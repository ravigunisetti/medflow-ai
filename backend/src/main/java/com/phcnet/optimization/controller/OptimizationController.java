package com.phcnet.optimization.controller;

import com.phcnet.common.dto.ApiResponse;
import com.phcnet.optimization.dto.OptimizationRequest;
import com.phcnet.optimization.dto.RedistributionRecommendationDTO;
import com.phcnet.optimization.service.OptimizationService;
import com.phcnet.transfer.dto.TransferDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/optimization")
@Tag(name = "Optimization Engine", description = "Deterministic solver for multi-constraint inter-PHC stock redistribution")
public class OptimizationController {

    private final OptimizationService optimizationService;

    public OptimizationController(OptimizationService optimizationService) {
        this.optimizationService = optimizationService;
    }

    @PostMapping("/redistribute")
    @Operation(summary = "Run deterministic redistribution solver to match deficit PHCs with nearby surplus donors")
    public ResponseEntity<ApiResponse<List<RedistributionRecommendationDTO>>> calculateRedistributions(
            @RequestBody(required = false) OptimizationRequest request) {
        List<RedistributionRecommendationDTO> recommendations = optimizationService.calculateRedistributions(request);
        return ResponseEntity.ok(ApiResponse.ok("Redistribution optimization completed", recommendations));
    }

    @PostMapping("/apply-recommendations")
    @Operation(summary = "Commit optimized recommendations into pending transfer records for administrative approval")
    public ResponseEntity<ApiResponse<List<TransferDTO>>> applyRecommendations(
            @RequestBody List<RedistributionRecommendationDTO> recommendations) {
        List<TransferDTO> created = optimizationService.applyRecommendations(recommendations);
        return ResponseEntity.ok(ApiResponse.ok("Recommendations applied into pending transfers", created));
    }
}
