package com.phcnet.prediction.controller;

import com.phcnet.common.dto.ApiResponse;
import com.phcnet.common.dto.PageResponse;
import com.phcnet.prediction.dto.AlertDTO;
import com.phcnet.prediction.dto.PredictionDTO;
import com.phcnet.prediction.dto.RiskExplanationDTO;
import com.phcnet.prediction.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Predictions & Alerts", description = "Endpoints for deterministic stock-out risk ratings and critical shortage alerts")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/predictions")
    @Operation(summary = "Get stock-out risk predictions with optional risk filter (CRITICAL, HIGH, MEDIUM, LOW)")
    public ResponseEntity<ApiResponse<PageResponse<PredictionDTO>>> getPredictions(
            @RequestParam(required = false) String riskLevel,
            @PageableDefault(size = 20, sort = "predictedDaysToStockout", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(predictionService.getPredictions(riskLevel, pageable))));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get list of active critical and high-risk alerts across all PHCs")
    public ResponseEntity<ApiResponse<List<AlertDTO>>> getActiveAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(predictionService.getActiveAlerts()));
    }

    @GetMapping("/predictions/explain")
    @Operation(summary = "Get exact mathematical formula and human-readable derivation of risk calculation for a PHC and medicine")
    public ResponseEntity<ApiResponse<RiskExplanationDTO>> explainRisk(
            @RequestParam Long phcId,
            @RequestParam Long medicineId) {
        return ResponseEntity.ok(ApiResponse.ok(predictionService.explainRisk(phcId, medicineId)));
    }

    @PostMapping("/predictions/calculate")
    @Operation(summary = "Trigger deterministic risk calculation for a specific PHC and medicine")
    public ResponseEntity<ApiResponse<PredictionDTO>> calculateRisk(
            @RequestParam Long phcId,
            @RequestParam Long medicineId) {
        PredictionDTO dto = predictionService.calculateAndSaveRisk(phcId, medicineId);
        return ResponseEntity.ok(ApiResponse.ok("Risk evaluated", dto));
    }

    @PostMapping("/predictions/evaluate-all")
    @Operation(summary = "Trigger network-wide deterministic risk evaluation across all PHCs and medicines")
    public ResponseEntity<ApiResponse<String>> evaluateAll() {
        predictionService.evaluateAllFacilities();
        return ResponseEntity.ok(ApiResponse.ok("Network-wide evaluation completed", "OK"));
    }
}
