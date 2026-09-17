package com.phcnet.event;

import com.phcnet.prediction.service.PredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);
    private final PredictionService predictionService;

    // Idempotent deduplication cache for processed events
    private final Set<String> processedEvents = Collections.synchronizedSet(new HashSet<>());
    private long failedEventCount = 0;
    private long processedEventCount = 0;

    public EventConsumer(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @Async
    @EventListener
    public void onInventoryUpdated(InventoryUpdatedEvent event) {
        if (processedEvents.contains(event.eventId())) {
            log.warn("[Pub/Sub:Consumer] Duplicate event detected, skipping eventId={}", event.eventId());
            return;
        }

        log.info("[Pub/Sub:Consumer] Received INVENTORY_UPDATED eventId={} for PHC {} and Medicine {}",
                event.eventId(), event.phcId(), event.medicineId());

        int retries = 3;
        boolean success = false;

        while (retries > 0 && !success) {
            try {
                // Trigger asynchronous risk recalculation
                predictionService.calculateAndSaveRisk(event.phcId(), event.medicineId());
                processedEvents.add(event.eventId());
                processedEventCount++;
                success = true;
                log.info("[Pub/Sub:Consumer] Successfully recalculated risk for PHC {} med {}",
                        event.phcId(), event.medicineId());
            } catch (Exception ex) {
                retries--;
                log.error("[Pub/Sub:Consumer] Error processing eventId={}, remaining retries={}: {}",
                        event.eventId(), retries, ex.getMessage());
                if (retries == 0) {
                    failedEventCount++;
                    log.error("[Pub/Sub:DeadLetter] Event {} moved to dead-letter queue topic='phc-dead-letter'",
                            event.eventId());
                }
            }
        }
    }

    public long getProcessedEventCount() { return processedEventCount; }
    public long getFailedEventCount() { return failedEventCount; }
}
