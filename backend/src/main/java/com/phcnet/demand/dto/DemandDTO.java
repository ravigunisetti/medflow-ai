package com.phcnet.demand.dto;

import com.phcnet.demand.model.Demand;
import java.time.LocalDate;

public record DemandDTO(
    Long id,
    Long phcId,
    String phcName,
    Long medicineId,
    String medicineCode,
    String medicineName,
    LocalDate recordDate,
    Integer quantityUsed
) {
    public static DemandDTO from(Demand d) {
        return new DemandDTO(
            d.getId(),
            d.getPhc().getId(),
            d.getPhc().getName(),
            d.getMedicine().getId(),
            d.getMedicine().getCode(),
            d.getMedicine().getName(),
            d.getRecordDate(),
            d.getQuantityUsed()
        );
    }
}
