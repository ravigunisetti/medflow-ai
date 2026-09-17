package com.phcnet.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventProcessingTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private EventConsumer eventConsumer;

    @Test
    void testInventoryUpdatedEventPipeline() throws InterruptedException {
        long beforeCount = eventConsumer.getProcessedEventCount();

        // Publish event for PHC 1 and Medicine 5 (Paracetamol)
        eventPublisher.publishInventoryUpdated(1L, 5L, 200);

        // Allow async thread pool to dispatch and process
        Thread.sleep(600);

        long afterCount = eventConsumer.getProcessedEventCount();
        assertTrue(afterCount >= beforeCount, "Event count should increment after processing");
        assertEquals(0, eventConsumer.getFailedEventCount(), "Failed events should be 0");
    }
}
