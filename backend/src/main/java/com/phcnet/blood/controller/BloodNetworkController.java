package com.phcnet.blood.controller;

import com.phcnet.blood.dto.*;
import com.phcnet.blood.service.BloodNetworkService;
import com.phcnet.common.dto.ApiResponse;
import com.phcnet.security.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blood")
@Tag(name = "Emergency Blood Network", description = "Endpoints for blood banks, real-time inventory, emergency blood requests, deterministic matching, and cold-chain transfers")
public class BloodNetworkController {

    private final BloodNetworkService bloodNetworkService;

    public BloodNetworkController(BloodNetworkService bloodNetworkService) {
        this.bloodNetworkService = bloodNetworkService;
    }

    @GetMapping("/banks")
    @Operation(summary = "Get list of blood banks, optionally filtered by district")
    public ResponseEntity<ApiResponse<List<BloodBankDTO>>> getBloodBanks(
            @RequestParam(required = false) String district) {
        return ResponseEntity.ok(ApiResponse.ok(bloodNetworkService.getAllBloodBanks(district)));
    }

    @GetMapping("/banks/{id}")
    @Operation(summary = "Get single blood bank details by ID")
    public ResponseEntity<ApiResponse<BloodBankDTO>> getBloodBankById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodNetworkService.getBloodBankById(id)));
    }

    @GetMapping("/banks/{id}/inventory")
    @Operation(summary = "Get inventory levels for a specific blood bank")
    public ResponseEntity<ApiResponse<List<BloodInventoryDTO>>> getBloodBankInventory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodNetworkService.getBloodBankInventory(id)));
    }

    @GetMapping("/inventory")
    @Operation(summary = "Get all blood inventory records across all blood banks in the network")
    public ResponseEntity<ApiResponse<List<BloodInventoryDTO>>> getAllInventories() {
        return ResponseEntity.ok(ApiResponse.ok(bloodNetworkService.getAllBloodInventories()));
    }

    @GetMapping("/requests")
    @Operation(summary = "List all emergency blood requests")
    public ResponseEntity<ApiResponse<List<EmergencyBloodRequestDTO>>> getAllRequests() {
        return ResponseEntity.ok(ApiResponse.ok(bloodNetworkService.getAllRequests()));
    }

    @GetMapping("/requests/{id}")
    @Operation(summary = "Get single emergency blood request by ID")
    public ResponseEntity<ApiResponse<EmergencyBloodRequestDTO>> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodNetworkService.getRequestById(id)));
    }

    @PostMapping("/requests")
    @Operation(summary = "Create an urgent emergency blood request for a hospital or PHC")
    public ResponseEntity<ApiResponse<EmergencyBloodRequestDTO>> createRequest(
            @Valid @RequestBody CreateBloodRequestDTO dto,
            @AuthenticationPrincipal User user) {
        EmergencyBloodRequestDTO created = bloodNetworkService.createEmergencyRequest(dto, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Emergency blood request initiated", created));
    }

    @GetMapping("/requests/{id}/matches")
    @Operation(summary = "Run deterministic matching engine to identify compatible blood resources, ETAs, and top recommended source")
    public ResponseEntity<ApiResponse<BloodMatchingResultDTO>> getMatchesForRequest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodNetworkService.findMatchesForRequest(id)));
    }

    @PostMapping("/transfers/confirm")
    @Operation(summary = "Confirm blood allocation and initiate dispatch transfer from blood bank to hospital")
    public ResponseEntity<ApiResponse<BloodTransferDTO>> confirmTransfer(
            @Valid @RequestBody ConfirmTransferRequestDTO dto) {
        BloodTransferDTO transfer = bloodNetworkService.confirmTransfer(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Blood allocation confirmed and transfer dispatched", transfer));
    }

    @PatchMapping("/transfers/{id}/status")
    @Operation(summary = "Update transfer tracking status (e.g. IN_TRANSIT, DELIVERED, CANCELLED)")
    public ResponseEntity<ApiResponse<BloodTransferDTO>> updateTransferStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        BloodTransferDTO updated = bloodNetworkService.updateTransferStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Transfer status updated", updated));
    }

    @GetMapping("/transfers")
    @Operation(summary = "List all active and historical blood transfers")
    public ResponseEntity<ApiResponse<List<BloodTransferDTO>>> getAllTransfers() {
        return ResponseEntity.ok(ApiResponse.ok(bloodNetworkService.getAllTransfers()));
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Get aggregated Command Center dashboard metrics for the Emergency Blood Network")
    public ResponseEntity<ApiResponse<BloodDashboardSummaryDTO>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.ok(bloodNetworkService.getDashboardSummary()));
    }

    @PostMapping("/simulate")
    @Operation(summary = "Run interactive emergency blood scenario simulation (e.g. Mass Casualty, Trauma, Surgical Need)")
    public ResponseEntity<ApiResponse<BloodMatchingResultDTO>> simulateEmergency(
            @RequestBody BloodSimulationRequestDTO dto) {
        BloodMatchingResultDTO result = bloodNetworkService.runEmergencySimulation(dto);
        return ResponseEntity.ok(ApiResponse.ok("Emergency scenario simulated", result));
    }
}
