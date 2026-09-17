package com.phcnet.inventory.dto;

import com.phcnet.inventory.model.Inventory;
import java.time.LocalDate;

public record InventoryDTO(
    Long id,
    Long phcId,
    String phcName,
    String district,
    Long medicineId,
    String medicineCode,
    String medicineName,
    String category,
    String unit,
    Integer safetyStock,
    Integer quantity,
    Integer reservedQuantity,
    Integer availableQuantity,
    String batchNumber,
    LocalDate expiryDate
) {
    public static InventoryDTO from(Inventory inv) {
        int avail = Math.max(0, inv.getQuantity() - (inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0));
        return new InventoryDTO(
            inv.getId(),
            inv.getPhc().getId(),
            inv.getPhc().getName(),
            inv.getPhc().getDistrict(),
            inv.getMedicine().getId(),
            inv.getMedicine().getCode(),
            inv.getMedicine().getName(),
            inv.getMedicine().getCategory(),
            inv.getMedicine().getUnit(),
            inv.getMedicine().getSafetyStock(),
            inv.getQuantity(),
            inv.getReservedQuantity(),
            avail,
            inv.getBatchNumber(),
            inv.getExpiryDate()
        );
    }
}
