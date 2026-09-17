package com.phcnet.event;

import java.time.Instant;
import java.util.UUID;

public record AlertRaisedEvent(
    String eventId,
    Long phcId,
    Long medicineId,
    String riskLevel,
    double daysRemaining,
    Instant timestamp
) {
    public static AlertRaisedEvent of(Long phcId, Long medicineId, String riskLevel, double daysRemaining) {
        return new AlertRaisedEvent(
            UUID.randomUUID().toString(),
            phcId,
            medicineId,
            riskLevel,
            daysRemaining,
            Instant.now()
        );
    }
}
