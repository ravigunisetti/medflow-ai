package com.phcnet.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferCreateRequest(
    @NotNull(message = "Source PHC is required")
    Long sourcePhcId,

    @NotNull(message = "Destination PHC is required")
    Long destinationPhcId,

    @NotNull(message = "Medicine is required")
    Long medicineId,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    Integer quantity,

    Double distanceKm,
    Double estimatedCost,
    String recommendationReason
) {}
