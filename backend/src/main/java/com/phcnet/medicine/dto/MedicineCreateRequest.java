package com.phcnet.medicine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MedicineCreateRequest(
    @NotBlank(message = "Medicine code is required")
    String code,

    @NotBlank(message = "Medicine name is required")
    String name,

    @NotBlank(message = "Category is required")
    String category,

    @NotBlank(message = "Unit of measurement is required")
    String unit,

    @NotNull(message = "Safety stock threshold is required")
    @Positive(message = "Safety stock must be positive")
    Integer safetyStock,

    Integer shelfLifeDays,
    Boolean requiresColdChain
) {}
