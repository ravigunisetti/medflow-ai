package com.phcnet.demand.dto;

import com.phcnet.demand.model.PatientFootfall;
import java.time.LocalDate;

public record FootfallDTO(
    Long id,
    Long phcId,
    String phcName,
    LocalDate recordDate,
    Integer patientCount,
    Integer emergencyCount
) {
    public static FootfallDTO from(PatientFootfall pf) {
        return new FootfallDTO(
            pf.getId(),
            pf.getPhc().getId(),
            pf.getPhc().getName(),
            pf.getRecordDate(),
            pf.getPatientCount(),
            pf.getEmergencyCount()
        );
    }
}
