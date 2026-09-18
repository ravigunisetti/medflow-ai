package com.phcnet.blood.dto;

import com.phcnet.blood.model.BloodTransfer;
import java.time.Instant;

public record BloodTransferDTO(
        Long id,
        Long requestId,
        String bloodGroup,
        Integer units,
        Long sourceBloodBankId,
        String sourceBloodBankName,
        String sourceDistrict,
        Long destinationHospitalId,
        String destinationHospitalName,
        String destinationDistrict,
        Double estimatedDistanceKm,
        Integer estimatedEtaMinutes,
        String status,
        Boolean coldChainVerified,
        Instant dispatchedAt,
        Instant deliveredAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static BloodTransferDTO fromEntity(BloodTransfer entity) {
        if (entity == null) return null;
        return new BloodTransferDTO(
                entity.getId(),
                entity.getRequest() != null ? entity.getRequest().getId() : null,
                entity.getRequest() != null ? entity.getRequest().getBloodGroup() : null,
                entity.getUnits(),
                entity.getSourceBloodBank() != null ? entity.getSourceBloodBank().getId() : null,
                entity.getSourceBloodBank() != null ? entity.getSourceBloodBank().getName() : null,
                entity.getSourceBloodBank() != null ? entity.getSourceBloodBank().getDistrict() : null,
                entity.getDestinationHospital() != null ? entity.getDestinationHospital().getId() : null,
                entity.getDestinationHospital() != null ? entity.getDestinationHospital().getName() : null,
                entity.getDestinationHospital() != null ? entity.getDestinationHospital().getDistrict() : null,
                entity.getEstimatedDistanceKm(),
                entity.getEstimatedEtaMinutes(),
                entity.getStatus(),
                entity.getColdChainVerified(),
                entity.getDispatchedAt(),
                entity.getDeliveredAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
