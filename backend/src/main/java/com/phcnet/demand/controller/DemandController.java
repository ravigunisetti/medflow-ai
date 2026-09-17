package com.phcnet.demand.controller;

import com.phcnet.common.dto.ApiResponse;
import com.phcnet.common.dto.PageResponse;
import com.phcnet.demand.dto.DemandDTO;
import com.phcnet.demand.dto.FootfallDTO;
import com.phcnet.demand.service.DemandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Demand & Footfall", description = "Endpoints for medicine consumption and patient OPD footfall records")
public class DemandController {

    private final DemandService demandService;

    public DemandController(DemandService demandService) {
        this.demandService = demandService;
    }

    @GetMapping("/demands")
    @Operation(summary = "Get historical medicine demands with pagination")
    public ResponseEntity<ApiResponse<PageResponse<DemandDTO>>> getDemands(
            @RequestParam(required = false) Long phcId,
            @RequestParam(required = false) Long medicineId,
            @PageableDefault(size = 20, sort = "recordDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(demandService.getDemands(phcId, medicineId, pageable))));
    }

    @GetMapping("/demands/history")
    @Operation(summary = "Get historical demand records for a specific PHC and medicine over N days")
    public ResponseEntity<ApiResponse<List<DemandDTO>>> getDemandHistory(
            @RequestParam Long phcId,
            @RequestParam Long medicineId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.ok(demandService.getDemandHistory(phcId, medicineId, days)));
    }

    @PostMapping("/demands")
    @Operation(summary = "Record daily medicine dispensing")
    public ResponseEntity<ApiResponse<DemandDTO>> recordDemand(
            @RequestParam Long phcId,
            @RequestParam Long medicineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer quantity) {
        DemandDTO dto = demandService.recordDemand(phcId, medicineId, date, quantity);
        return ResponseEntity.ok(ApiResponse.ok("Demand recorded", dto));
    }

    @GetMapping("/footfall")
    @Operation(summary = "Get patient footfall records with pagination")
    public ResponseEntity<ApiResponse<PageResponse<FootfallDTO>>> getFootfalls(
            @RequestParam(required = false) Long phcId,
            @PageableDefault(size = 20, sort = "recordDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(demandService.getFootfalls(phcId, pageable))));
    }

    @GetMapping("/footfall/history")
    @Operation(summary = "Get footfall history for a specific PHC over N days")
    public ResponseEntity<ApiResponse<List<FootfallDTO>>> getFootfallHistory(
            @RequestParam Long phcId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.ok(demandService.getFootfallHistory(phcId, days)));
    }

    @PostMapping("/footfall")
    @Operation(summary = "Record daily patient footfall")
    public ResponseEntity<ApiResponse<FootfallDTO>> recordFootfall(
            @RequestParam Long phcId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer patientCount,
            @RequestParam(defaultValue = "0") Integer emergencyCount) {
        FootfallDTO dto = demandService.recordFootfall(phcId, date, patientCount, emergencyCount);
        return ResponseEntity.ok(ApiResponse.ok("Footfall recorded", dto));
    }
}
