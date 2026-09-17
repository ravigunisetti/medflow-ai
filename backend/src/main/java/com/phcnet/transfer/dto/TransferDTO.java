package com.phcnet.transfer.dto;

import com.phcnet.transfer.model.Transfer;
import java.time.Instant;

public record TransferDTO(
    Long id,
    Long sourcePhcId,
    String sourcePhcName,
    String sourceDistrict,
    Long destinationPhcId,
    String destinationPhcName,
    String destinationDistrict,
    Long medicineId,
    String medicineName,
    String medicineUnit,
    Integer quantity,
    Double distanceKm,
    Double estimatedCost,
    String status,
    String recommendationReason,
    Long approvedByUserId,
    Instant createdAt,
    Instant updatedAt
) {
    public static TransferDTO from(Transfer t) {
        return new TransferDTO(
            t.getId(),
            t.getSourcePhc().getId(),
            t.getSourcePhc().getName(),
            t.getSourcePhc().getDistrict(),
            t.getDestinationPhc().getId(),
            t.getDestinationPhc().getName(),
            t.getDestinationPhc().getDistrict(),
            t.getMedicine().getId(),
            t.getMedicine().getName(),
            t.getMedicine().getUnit(),
            t.getQuantity(),
            t.getDistanceKm(),
            t.getEstimatedCost(),
            t.getStatus(),
            t.getRecommendationReason(),
            t.getApprovedByUserId(),
            t.getCreatedAt(),
            t.getUpdatedAt()
        );
    }
}
