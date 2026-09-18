package com.phcnet.blood.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfirmTransferRequestDTO(
        @NotNull(message = "Request ID is required")
        Long requestId,

        @NotNull(message = "Source Blood Bank ID is required")
        Long sourceBloodBankId,

        @NotNull(message = "Units to allocate is required")
        @Min(value = 1, message = "Units must be at least 1")
        Integer units,

        Double estimatedDistanceKm,
        Integer estimatedEtaMinutes
) {}
