package com.phcnet.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final ApplicationEventPublisher applicationEventPublisher;

    public EventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishInventoryUpdated(Long phcId, Long medicineId, int quantity) {
        InventoryUpdatedEvent event = InventoryUpdatedEvent.of(phcId, medicineId, quantity);
        log.info("[Pub/Sub:Publish] INVENTORY_UPDATED topic='phc-inventory-updates' eventId={} phcId={} medicineId={} qty={}",
                event.eventId(), event.phcId(), event.medicineId(), event.quantity());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishAlertRaised(Long phcId, Long medicineId, String riskLevel, double daysRemaining) {
        AlertRaisedEvent event = AlertRaisedEvent.of(phcId, medicineId, riskLevel, daysRemaining);
        log.warn("[Pub/Sub:Publish] ALERT_RAISED topic='phc-alerts' eventId={} phcId={} risk={} daysRemaining={}",
                event.eventId(), event.phcId(), event.riskLevel(), event.daysRemaining());
        applicationEventPublisher.publishEvent(event);
    }
}
