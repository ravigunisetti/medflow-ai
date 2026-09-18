package com.phcnet.blood.dto;

import com.phcnet.blood.model.BloodBank;
import com.phcnet.blood.model.VerificationStatus;
import java.time.Instant;

public record BloodBankDTO(
        Long id,
        String name,
        String district,
        String state,
        Double latitude,
        Double longitude,
        VerificationStatus verificationStatus,
        String contactPhone,
        String contactEmail,
        String operatingHours,
        Integer storageCapacityUnits,
        Boolean isActive,
        Instant createdAt
) {
    public static BloodBankDTO fromEntity(BloodBank entity) {
        if (entity == null) return null;
        return new BloodBankDTO(
                entity.getId(),
                entity.getName(),
                entity.getDistrict(),
                entity.getState(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getVerificationStatus(),
                entity.getContactPhone(),
                entity.getContactEmail(),
                entity.getOperatingHours(),
                entity.getStorageCapacityUnits(),
                entity.getIsActive(),
                entity.getCreatedAt()
        );
    }
}
