package com.phcnet.phc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PhcCreateRequest(
    @NotBlank(message = "PHC name is required")
    String name,

    @NotBlank(message = "District is required")
    String district,

    String state,

    @NotNull(message = "Latitude is required")
    Double latitude,

    @NotNull(message = "Longitude is required")
    Double longitude,

    @NotNull(message = "Population served is required")
    @Positive(message = "Population served must be positive")
    Integer populationServed
) {}
