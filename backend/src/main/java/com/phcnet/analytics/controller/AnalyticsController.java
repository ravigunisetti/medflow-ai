package com.phcnet.analytics.controller;

import com.phcnet.analytics.dto.AnalyticsReportDTO;
import com.phcnet.analytics.service.AnalyticsService;
import com.phcnet.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "BigQuery & Longitudinal Analytics", description = "Analytical reporting endpoints answering multi-district epidemiology and supply chain efficiency questions")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/comprehensive-report")
    @Operation(summary = "Get analytical report answering district risk ranks, top medicines, overstock PHCs, seasonal patterns, and transfer ROI")
    public ResponseEntity<ApiResponse<AnalyticsReportDTO>> getComprehensiveReport() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.generateComprehensiveReport()));
    }
}
