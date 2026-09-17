package com.phcnet.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record InventoryUpdateRequest(
    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity cannot be negative")
    Integer quantity,

    @PositiveOrZero(message = "Reserved quantity cannot be negative")
    Integer reservedQuantity,

    String batchNumber,
    LocalDate expiryDate
) {}
