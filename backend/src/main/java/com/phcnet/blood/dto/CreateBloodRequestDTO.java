package com.phcnet.blood.dto;

import com.phcnet.blood.model.RequestPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateBloodRequestDTO(
        @NotNull(message = "Hospital ID is required")
        Long hospitalId,

        @NotBlank(message = "Blood group is required")
        String bloodGroup,

        String componentType,

        @NotNull(message = "Units required cannot be null")
        @Min(value = 1, message = "At least 1 unit must be requested")
        Integer unitsRequired,

        RequestPriority priority,

        @NotNull(message = "Required-by deadline is required")
        Instant requiredBy,

        String clinicalNotes
) {}
