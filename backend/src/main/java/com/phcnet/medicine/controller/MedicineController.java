package com.phcnet.medicine.controller;

import com.phcnet.common.dto.ApiResponse;
import com.phcnet.common.dto.PageResponse;
import com.phcnet.medicine.dto.MedicineCreateRequest;
import com.phcnet.medicine.dto.MedicineDTO;
import com.phcnet.medicine.service.MedicineService;
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
@RequestMapping("/api/medicines")
@Tag(name = "Medicine Catalog", description = "Endpoints for Essential Medicine List (EML) catalog items")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping
    @Operation(summary = "Get all medicines with pagination and optional category filter")
    public ResponseEntity<ApiResponse<PageResponse<MedicineDTO>>> getAllMedicines(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(medicineService.getAllMedicines(category, pageable))));
    }

    @GetMapping("/all")
    @Operation(summary = "Get complete list of medicines (unpaginated)")
    public ResponseEntity<ApiResponse<List<MedicineDTO>>> getAllMedicinesList() {
        return ResponseEntity.ok(ApiResponse.ok(medicineService.getAllMedicinesList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get medicine by ID")
    public ResponseEntity<ApiResponse<MedicineDTO>> getMedicineById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(medicineService.getMedicineById(id)));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get medicine by unique catalog code")
    public ResponseEntity<ApiResponse<MedicineDTO>> getMedicineByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(medicineService.getMedicineByCode(code)));
    }

    @PostMapping
    @Operation(summary = "Add a new medicine to the catalog")
    public ResponseEntity<ApiResponse<MedicineDTO>> createMedicine(@Valid @RequestBody MedicineCreateRequest request) {
        MedicineDTO created = medicineService.createMedicine(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Medicine created successfully", created));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a medicine from the catalog")
    public ResponseEntity<ApiResponse<Void>> deleteMedicine(@PathVariable Long id) {
        medicineService.deleteMedicine(id);
        return ResponseEntity.ok(ApiResponse.ok("Medicine deleted successfully", null));
    }
}
