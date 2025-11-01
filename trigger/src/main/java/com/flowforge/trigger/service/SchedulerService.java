package com.flowforge.trigger.service;

import com.flowforge.trigger.dto.TriggerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling scheduled triggers
 * This is a simple implementation - in production you might want to:
 * - Store schedule configs in database
 * - Use dynamic scheduling (Quartz, etc.)
 * - Allow users to configure cron expressions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final TriggerService triggerService;

    /**
     * Example: Run every minute
     * Cron format: second, minute, hour, day, month, weekday
     */
    @Scheduled(cron = "0 * * * * ?")
    public void everyMinuteSchedule() {
        log.debug("Running every minute schedule");
        
        TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .triggerType("schedule.every_minute")
                .payload(Map.of(
                    "scheduleName", "every_minute",
                    "executionTime", Instant.now().toString()
                ))
                .metadata(Map.of("source", "scheduler"))
                .timestamp(Instant.now())
                .build();
        
        triggerService.processTrigger(event);
    }

    /**
     * Example: Run every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void everyFiveMinutesSchedule() {
        log.debug("Running every 5 minutes schedule");
        
        TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .triggerType("schedule.every_5_minutes")
                .payload(Map.of(
                    "scheduleName", "every_5_minutes",
                    "executionTime", Instant.now().toString()
                ))
                .metadata(Map.of("source", "scheduler"))
                .timestamp(Instant.now())
                .build();
        
        triggerService.processTrigger(event);
    }

    /**
     * Example: Run every hour
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void everyHourSchedule() {
        log.info("Running every hour schedule");
        
        TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .triggerType("schedule.hourly")
                .payload(Map.of(
                    "scheduleName", "hourly",
                    "executionTime", Instant.now().toString()
                ))
                .metadata(Map.of("source", "scheduler"))
                .timestamp(Instant.now())
                .build();
        
        triggerService.processTrigger(event);
    }

    /**
     * Example: Run daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailySchedule() {
        log.info("Running daily schedule");
        
        TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .triggerType("schedule.daily")
                .payload(Map.of(
                    "scheduleName", "daily",
                    "executionTime", Instant.now().toString()
                ))
                .metadata(Map.of("source", "scheduler"))
                .timestamp(Instant.now())
                .build();
        
        triggerService.processTrigger(event);
    }

    /**
     * Trigger a custom schedule manually
     */
    public String triggerCustomSchedule(String scheduleName, Map<String, Object> config) {
        log.info("Manually triggering custom schedule: {}", scheduleName);
        
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>(config);
        payload.put("scheduleName", scheduleName);
        payload.put("executionTime", Instant.now().toString());
        payload.put("manual", true);
        
        TriggerEvent event = TriggerEvent.builder()
                .eventId(eventId)
                .triggerType("schedule." + scheduleName)
                .payload(payload)
                .metadata(Map.of(
                    "source", "scheduler",
                    "manual", "true"
                ))
                .timestamp(Instant.now())
                .build();
        
        triggerService.processTrigger(event);
        return eventId;
    }
}