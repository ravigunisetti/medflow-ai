package com.phcnet.phc.controller;

import com.phcnet.common.dto.ApiResponse;
import com.phcnet.common.dto.PageResponse;
import com.phcnet.phc.dto.PhcCreateRequest;
import com.phcnet.phc.dto.PhcDTO;
import com.phcnet.phc.service.PhcService;
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
@RequestMapping("/api/phcs")
@Tag(name = "PHC Management", description = "Endpoints for managing Primary Health Centres")
public class PhcController {

    private final PhcService phcService;

    public PhcController(PhcService phcService) {
        this.phcService = phcService;
    }

    @GetMapping
    @Operation(summary = "Get all PHCs with pagination and optional district filter")
    public ResponseEntity<ApiResponse<PageResponse<PhcDTO>>> getAllPhcs(
            @RequestParam(required = false) String district,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(phcService.getAllPhcs(district, pageable))));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active PHCs as a complete list (for map plotting)")
    public ResponseEntity<ApiResponse<List<PhcDTO>>> getAllActivePhcs() {
        return ResponseEntity.ok(ApiResponse.ok(phcService.getAllActivePhcs()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single PHC by unique ID")
    public ResponseEntity<ApiResponse<PhcDTO>> getPhcById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(phcService.getPhcById(id)));
    }

    @PostMapping
    @Operation(summary = "Register a new PHC facility")
    public ResponseEntity<ApiResponse<PhcDTO>> createPhc(@Valid @RequestBody PhcCreateRequest request) {
        PhcDTO created = phcService.createPhc(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("PHC registered successfully", created));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "De-register / remove a PHC")
    public ResponseEntity<ApiResponse<Void>> deletePhc(@PathVariable Long id) {
        phcService.deletePhc(id);
        return ResponseEntity.ok(ApiResponse.ok("PHC removed successfully", null));
    }
}
