package com.flowforge.trigger.plugin.impl;

import com.flowforge.trigger.dto.TriggerEvent;
import com.flowforge.trigger.plugin.BaseTriggerPlugin;
import com.flowforge.trigger.service.TriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Built-in schedule trigger plugin.
 * Handles cron-based scheduled triggers.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduleTriggerPlugin extends BaseTriggerPlugin {
    
    private final TaskScheduler taskScheduler;
    private final TriggerService triggerService;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    @Override
    public String getType() {
        return "schedule";
    }
    
    @Override
    public String getName() {
        return "Schedule Trigger";
    }
    
    @Override
    public String getDescription() {
        return "Triggers workflow based on a cron schedule";
    }
    
    @Override
    public Map<String, Object> getConfigSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Cron expression
        Map<String, Object> cronProp = new HashMap<>();
        cronProp.put("type", "string");
        cronProp.put("description", "Cron expression (e.g., '0 0 * * * ?' for hourly)");
        cronProp.put("example", "0 */5 * * * ?");
        properties.put("cronExpression", cronProp);
        
        // Timezone
        Map<String, Object> timezoneProp = new HashMap<>();
        timezoneProp.put("type", "string");
        timezoneProp.put("description", "Timezone for schedule");
        timezoneProp.put("default", "UTC");
        properties.put("timezone", timezoneProp);
        
        schema.put("properties", properties);
        schema.put("required", new String[]{"cronExpression"});
        
        return schema;
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return false;
        }
        
        if (!config.containsKey("cronExpression")) {
            log.error("Schedule trigger requires 'cronExpression'");
            return false;
        }
        
        try {
            String cronExpression = (String) config.get("cronExpression");
            new CronTrigger(cronExpression);
            return true;
        } catch (Exception e) {
            log.error("Invalid cron expression", e);
            return false;
        }
    }
    
    @Override
    public void start(String workflowId, Map<String, Object> config) throws Exception {
        super.start(workflowId, config);
        
        String cronExpression = (String) config.get("cronExpression");
        
        // Schedule the task
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
            () -> executeScheduledTrigger(workflowId, config),
            new CronTrigger(cronExpression)
        );
        
        scheduledTasks.put(workflowId, scheduledTask);
        
        log.info("Schedule trigger started for workflow: {} with cron: {}", 
            workflowId, cronExpression);
    }
    
    @Override
    public void stop(String workflowId) throws Exception {
        ScheduledFuture<?> scheduledTask = scheduledTasks.remove(workflowId);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            log.info("Schedule trigger stopped for workflow: {}", workflowId);
        }
        super.stop(workflowId);
    }
    
    private void executeScheduledTrigger(String workflowId, Map<String, Object> config) {
        try {
            log.debug("Executing scheduled trigger for workflow: {}", workflowId);
            
            TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .triggerType(getType())
                .payload(Map.of(
                    "workflowId", workflowId,
                    "cronExpression", config.get("cronExpression"),
                    "executionTime", Instant.now().toString()
                ))
                .metadata(Map.of(
                    "workflowId", workflowId,
                    "triggerSource", "schedule"
                ))
                .timestamp(Instant.now())
                .build();
            
            triggerService.processTrigger(event);
        } catch (Exception e) {
            log.error("Error executing scheduled trigger for workflow: {}", workflowId, e);
        }
    }
    
    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> status = super.getStatus();
        status.put("activeSchedules", scheduledTasks.size());
        status.put("schedules", activeWorkflows.entrySet().stream()
            .map(entry -> Map.of(
                "workflowId", entry.getKey(),
                "cronExpression", entry.getValue().get("cronExpression"),
                "active", scheduledTasks.containsKey(entry.getKey())
            ))
            .toList());
        return status;
    }
    
    @Override
    public void destroy() {
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
        super.destroy();
    }
}