package com.phcnet.phc.dto;

import com.phcnet.phc.model.Phc;

public record PhcDTO(
    Long id,
    String name,
    String district,
    String state,
    Double latitude,
    Double longitude,
    Integer populationServed,
    Boolean isActive
) {
    public static PhcDTO from(Phc p) {
        return new PhcDTO(
            p.getId(),
            p.getName(),
            p.getDistrict(),
            p.getState(),
            p.getLatitude(),
            p.getLongitude(),
            p.getPopulationServed(),
            p.getIsActive()
        );
    }
}
