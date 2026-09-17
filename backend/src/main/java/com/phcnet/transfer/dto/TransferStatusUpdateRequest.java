package com.phcnet.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TransferStatusUpdateRequest(
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "APPROVED|IN_TRANSIT|COMPLETED|REJECTED", message = "Status must be APPROVED, IN_TRANSIT, COMPLETED, or REJECTED")
    String status,

    Long approvedByUserId,
    String rejectionReason
) {}
