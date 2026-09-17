package com.phcnet.inventory.controller;

import com.phcnet.common.dto.ApiResponse;
import com.phcnet.common.dto.PageResponse;
import com.phcnet.inventory.dto.InventoryDTO;
import com.phcnet.inventory.dto.InventoryUpdateRequest;
import com.phcnet.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Management", description = "Endpoints for managing PHC medicine stock balances")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "Get inventory records with pagination and optional PHC/Medicine filters")
    public ResponseEntity<ApiResponse<PageResponse<InventoryDTO>>> getInventories(
            @RequestParam(required = false) Long phcId,
            @RequestParam(required = false) Long medicineId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(inventoryService.getInventories(phcId, medicineId, pageable))));
    }

    @GetMapping("/phc/{phcId}")
    @Operation(summary = "Get complete stock list for a specific PHC facility")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> getInventoryByPhc(@PathVariable Long phcId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getInventoryByPhc(phcId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific inventory balance by record ID")
    public ResponseEntity<ApiResponse<InventoryDTO>> getInventoryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getInventoryById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update stock quantity, batch, and expiry date for an inventory item")
    public ResponseEntity<ApiResponse<InventoryDTO>> updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryUpdateRequest request) {
        InventoryDTO updated = inventoryService.updateInventory(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Inventory balance updated successfully", updated));
    }

    @PostMapping("/upsert")
    @Operation(summary = "Set stock quantity for a PHC-medicine pair (creates if non-existent)")
    public ResponseEntity<ApiResponse<InventoryDTO>> upsertStock(
            @RequestParam Long phcId,
            @RequestParam Long medicineId,
            @RequestParam Integer quantity) {
        InventoryDTO saved = inventoryService.upsertStock(phcId, medicineId, quantity);
        return ResponseEntity.ok(ApiResponse.ok("Stock recorded", saved));
    }
}
