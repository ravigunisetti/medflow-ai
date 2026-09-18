package com.phcnet.blood.dto;

import com.phcnet.blood.model.EmergencyBloodRequest;
import com.phcnet.blood.model.RequestPriority;
import com.phcnet.blood.model.RequestStatus;
import java.time.Instant;

public record EmergencyBloodRequestDTO(
        Long id,
        Long hospitalId,
        String hospitalName,
        String hospitalDistrict,
        Double hospitalLatitude,
        Double hospitalLongitude,
        String bloodGroup,
        String componentType,
        Integer unitsRequired,
        RequestPriority priority,
        Instant requiredBy,
        RequestStatus status,
        String clinicalNotes,
        String createdByName,
        Instant createdAt,
        Instant updatedAt
) {
    public static EmergencyBloodRequestDTO fromEntity(EmergencyBloodRequest entity) {
        if (entity == null) return null;
        return new EmergencyBloodRequestDTO(
                entity.getId(),
                entity.getHospital() != null ? entity.getHospital().getId() : null,
                entity.getHospital() != null ? entity.getHospital().getName() : null,
                entity.getHospital() != null ? entity.getHospital().getDistrict() : null,
                entity.getHospital() != null ? entity.getHospital().getLatitude() : null,
                entity.getHospital() != null ? entity.getHospital().getLongitude() : null,
                entity.getBloodGroup(),
                entity.getComponentType(),
                entity.getUnitsRequired(),
                entity.getPriority(),
                entity.getRequiredBy(),
                entity.getStatus(),
                entity.getClinicalNotes(),
                entity.getCreatedByUser() != null ? entity.getCreatedByUser().getFullName() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
