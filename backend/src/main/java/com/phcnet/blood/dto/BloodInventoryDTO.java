package com.phcnet.blood.dto;

import com.phcnet.blood.model.BloodInventory;
import java.time.Instant;

public record BloodInventoryDTO(
        Long id,
        Long bloodBankId,
        String bloodBankName,
        String bloodGroup,
        String componentType,
        Integer unitsAvailable,
        Integer reservedUnits,
        Integer unreservedUnits,
        Instant lastUpdated
) {
    public static BloodInventoryDTO fromEntity(BloodInventory entity) {
        if (entity == null) return null;
        return new BloodInventoryDTO(
                entity.getId(),
                entity.getBloodBank() != null ? entity.getBloodBank().getId() : null,
                entity.getBloodBank() != null ? entity.getBloodBank().getName() : null,
                entity.getBloodGroup(),
                entity.getComponentType(),
                entity.getUnitsAvailable(),
                entity.getReservedUnits(),
                entity.getUnreservedUnits(),
                entity.getLastUpdated()
        );
    }
}
