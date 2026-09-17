package com.phcnet.medicine.dto;

import com.phcnet.medicine.model.Medicine;

public record MedicineDTO(
    Long id,
    String code,
    String name,
    String category,
    String unit,
    Integer safetyStock,
    Integer shelfLifeDays,
    Boolean requiresColdChain
) {
    public static MedicineDTO from(Medicine m) {
        return new MedicineDTO(
            m.getId(),
            m.getCode(),
            m.getName(),
            m.getCategory(),
            m.getUnit(),
            m.getSafetyStock(),
            m.getShelfLifeDays(),
            m.getRequiresColdChain()
        );
    }
}
