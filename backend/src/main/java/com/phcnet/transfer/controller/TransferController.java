package com.phcnet.transfer.controller;

import com.phcnet.common.dto.ApiResponse;
import com.phcnet.common.dto.PageResponse;
import com.phcnet.transfer.dto.TransferCreateRequest;
import com.phcnet.transfer.dto.TransferDTO;
import com.phcnet.transfer.dto.TransferStatusUpdateRequest;
import com.phcnet.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Stock Transfers", description = "Endpoints for inter-PHC medicine transfer proposals, reviews, and tracking")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping
    @Operation(summary = "Get transfers with pagination and optional status filter (PENDING_APPROVAL, APPROVED, IN_TRANSIT, COMPLETED, REJECTED)")
    public ResponseEntity<ApiResponse<PageResponse<TransferDTO>>> getTransfers(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(transferService.getTransfers(status, pageable))));
    }

    @GetMapping("/phc/{phcId}")
    @Operation(summary = "Get all transfers involving a specific PHC (as source or destination)")
    public ResponseEntity<ApiResponse<List<TransferDTO>>> getTransfersForPhc(@PathVariable Long phcId) {
        return ResponseEntity.ok(ApiResponse.ok(transferService.getTransfersForPhc(phcId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single transfer by ID")
    public ResponseEntity<ApiResponse<TransferDTO>> getTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(transferService.getTransferById(id)));
    }

    @PostMapping
    @Operation(summary = "Create an inter-PHC stock redistribution transfer")
    public ResponseEntity<ApiResponse<TransferDTO>> createTransfer(@Valid @RequestBody TransferCreateRequest request) {
        TransferDTO created = transferService.createTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transfer created successfully", created));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update status of a transfer (APPROVE, IN_TRANSIT, COMPLETE, REJECT)")
    public ResponseEntity<ApiResponse<TransferDTO>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody TransferStatusUpdateRequest request) {
        TransferDTO updated = transferService.updateTransferStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Transfer status updated", updated));
    }
}
