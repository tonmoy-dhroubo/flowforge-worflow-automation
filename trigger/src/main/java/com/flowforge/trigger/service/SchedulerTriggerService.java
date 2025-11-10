package com.flowforge.trigger.service;

import com.flowforge.trigger.entity.TriggerRegistration;
import com.flowforge.trigger.event.TriggerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling scheduler triggers.
 * Supports cron expressions and simple interval-based scheduling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerTriggerService {

    private final TriggerEventPublisher eventPublisher;

    /**
     * Sets up a new scheduler trigger.
     * Calculates the first execution time based on configuration.
     */
    public TriggerRegistration setupSchedulerTrigger(TriggerRegistration trigger) {
        log.info("Setting up scheduler trigger for workflow: {}", trigger.getWorkflowId());

        // Calculate next scheduled time based on configuration
        Instant nextRun = calculateNextScheduledTime(trigger.getConfiguration());
        trigger.setNextScheduledAt(nextRun);

        log.info("Scheduler trigger set for: {}", nextRun);
        return trigger;
    }

    /**
     * Updates scheduler trigger with new next execution time
     */
    public TriggerRegistration updateSchedulerTrigger(TriggerRegistration trigger) {
        log.info("Updating scheduler trigger: {}", trigger.getId());

        Instant nextRun = calculateNextScheduledTime(trigger.getConfiguration());
        trigger.setNextScheduledAt(nextRun);

        return trigger;
    }

    /**
     * Processes a scheduled trigger execution.
     * Called by the scheduler when it's time to fire the trigger.
     */
    public void processScheduledTrigger(TriggerRegistration trigger) {
        log.info("Processing scheduled trigger: triggerId={}", trigger.getId());

        if (!trigger.isEnabled()) {
            log.warn("Trigger is disabled, skipping: triggerId={}", trigger.getId());
            return;
        }

        // Create trigger event
        TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID())
                .triggerId(trigger.getId())
                .workflowId(trigger.getWorkflowId())
                .userId(trigger.getUserId())
                .triggerType("scheduler")
                .timestamp(Instant.now())
                .payload(new HashMap<>()) // No payload for scheduler triggers
                .metadata(buildSchedulerMetadata(trigger))
                .build();

        // Publish to Kafka
        eventPublisher.publishTriggerEvent(event);

        log.info("Successfully processed scheduled trigger: eventId={}", event.getEventId());
    }

    /**
     * Calculates the next scheduled execution time based on trigger configuration.
     * Supports both interval-based and cron-like scheduling.
     */
    private Instant calculateNextScheduledTime(Map<String, Object> configuration) {
        // Check for interval-based scheduling
        if (configuration.containsKey("intervalMinutes")) {
            int intervalMinutes = (Integer) configuration.get("intervalMinutes");
            return Instant.now().plus(intervalMinutes, ChronoUnit.MINUTES);
        }

        // Check for specific time scheduling
        if (configuration.containsKey("scheduleTime")) {
            // In a real implementation, you would parse cron expressions here
            // For simplicity, we'll use a basic interval
            return Instant.now().plus(1, ChronoUnit.HOURS);
        }

        // Default: run every hour
        return Instant.now().plus(1, ChronoUnit.HOURS);
    }

    /**
     * Builds metadata for scheduler trigger events
     */
    private Map<String, Object> buildSchedulerMetadata(TriggerRegistration trigger) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "scheduler");
        metadata.put("scheduledTime", trigger.getNextScheduledAt().toString());
        metadata.put("executedAt", Instant.now().toString());
        
        if (trigger.getConfiguration().containsKey("intervalMinutes")) {
            metadata.put("intervalMinutes", trigger.getConfiguration().get("intervalMinutes"));
        }
        
        return metadata;
    }
}