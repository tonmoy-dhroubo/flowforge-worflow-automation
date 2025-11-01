package com.flowforge.trigger.service;

import com.flowforge.trigger.dto.TriggerEvent;
import com.flowforge.trigger.kafka.producer.TriggerEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriggerService {

    private final TriggerEventProducer triggerEventProducer;

    /**
     * Process a trigger event by sending it to Kafka
     */
    public void processTrigger(TriggerEvent event) {
        log.info("Processing trigger event: {} of type: {}", event.getEventId(), event.getTriggerType());
        
        // Validate the event
        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        
        // Send to Kafka
        triggerEventProducer.sendTriggerEvent(event);
        
        log.info("Trigger event {} sent to Kafka successfully", event.getEventId());
    }

    /**
     * Process a trigger event synchronously (waits for Kafka confirmation)
     */
    public void processTriggerSync(TriggerEvent event) {
        log.info("Processing trigger event synchronously: {} of type: {}", 
            event.getEventId(), event.getTriggerType());
        
        // Validate the event
        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        
        // Send to Kafka synchronously
        triggerEventProducer.sendTriggerEventSync(event);
        
        log.info("Trigger event {} sent to Kafka successfully (sync)", event.getEventId());
    }

    /**
     * Create and process a manual trigger
     */
    public String createManualTrigger(String workflowId, Map<String, Object> payload) {
        log.info("Creating manual trigger for workflow: {}", workflowId);
        
        String eventId = UUID.randomUUID().toString();
        TriggerEvent event = TriggerEvent.builder()
                .eventId(eventId)
                .triggerType("manual")
                .payload(payload)
                .metadata(Map.of("workflowId", workflowId))
                .timestamp(Instant.now())
                .build();
        
        processTrigger(event);
        return eventId;
    }

    /**
     * Create and process a scheduled trigger
     */
    public String createScheduledTrigger(String workflowId, String scheduleName, Map<String, Object> config) {
        log.info("Creating scheduled trigger for workflow: {} with schedule: {}", workflowId, scheduleName);
        
        String eventId = UUID.randomUUID().toString();
        TriggerEvent event = TriggerEvent.builder()
                .eventId(eventId)
                .triggerType("schedule." + scheduleName)
                .payload(config)
                .metadata(Map.of(
                    "workflowId", workflowId,
                    "scheduleName", scheduleName
                ))
                .timestamp(Instant.now())
                .build();
        
        processTrigger(event);
        return eventId;
    }
}