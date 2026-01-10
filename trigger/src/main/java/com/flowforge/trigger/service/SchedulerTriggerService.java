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

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerTriggerService {

    private final TriggerEventPublisher eventPublisher;

    public TriggerRegistration setupSchedulerTrigger(TriggerRegistration trigger) {
        log.info("Setting up scheduler trigger for workflow: {}", trigger.getWorkflowId());

        Instant nextRun = calculateNextScheduledTime(trigger.getConfiguration());
        trigger.setNextScheduledAt(nextRun);

        log.info("Scheduler trigger set for: {}", nextRun);
        return trigger;
    }

    public TriggerRegistration updateSchedulerTrigger(TriggerRegistration trigger) {
        log.info("Updating scheduler trigger: {}", trigger.getId());

        Instant nextRun = calculateNextScheduledTime(trigger.getConfiguration());
        trigger.setNextScheduledAt(nextRun);

        return trigger;
    }

    public void processScheduledTrigger(TriggerRegistration trigger) {
        log.info("Processing scheduled trigger: triggerId={}", trigger.getId());

        if (!trigger.isEnabled()) {
            log.warn("Trigger is disabled, skipping: triggerId={}", trigger.getId());
            return;
        }

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

        eventPublisher.publishTriggerEvent(event);

        log.info("Successfully processed scheduled trigger: eventId={}", event.getEventId());
    }

    private Instant calculateNextScheduledTime(Map<String, Object> configuration) {
        if (configuration.containsKey("intervalMinutes")) {
            int intervalMinutes = (Integer) configuration.get("intervalMinutes");
            return Instant.now().plus(intervalMinutes, ChronoUnit.MINUTES);
        }

        if (configuration.containsKey("scheduleTime")) {
            return Instant.now().plus(1, ChronoUnit.HOURS);
        }

        return Instant.now().plus(1, ChronoUnit.HOURS);
    }

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
