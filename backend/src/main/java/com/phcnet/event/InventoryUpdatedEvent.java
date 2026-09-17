package com.phcnet.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryUpdatedEvent(
    String eventId,
    Long phcId,
    Long medicineId,
    int quantity,
    Instant timestamp
) {
    public static InventoryUpdatedEvent of(Long phcId, Long medicineId, int quantity) {
        return new InventoryUpdatedEvent(
            UUID.randomUUID().toString(),
            phcId,
            medicineId,
            quantity,
            Instant.now()
        );
    }
}
