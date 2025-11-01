package com.flowforge.trigger.plugin;

import com.flowforge.trigger.dto.TriggerEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for trigger plugins.
 * Provides common functionality and default implementations.
 */
@Slf4j
public abstract class BaseTriggerPlugin implements TriggerPlugin {
    
    protected final Map<String, Map<String, Object>> activeWorkflows = new ConcurrentHashMap<>();
    protected boolean initialized = false;
    protected boolean healthy = true;
    
    @Override
    public void initialize(Map<String, Object> config) throws Exception {
        log.info("Initializing trigger plugin: {}", getType());
        this.initialized = true;
    }
    
    @Override
    public void start(String workflowId, Map<String, Object> config) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Plugin not initialized");
        }
        
        if (!validateConfig(config)) {
            throw new IllegalArgumentException("Invalid configuration for trigger: " + getType());
        }
        
        log.info("Starting trigger {} for workflow: {}", getType(), workflowId);
        activeWorkflows.put(workflowId, config);
    }
    
    @Override
    public void stop(String workflowId) throws Exception {
        log.info("Stopping trigger {} for workflow: {}", getType(), workflowId);
        activeWorkflows.remove(workflowId);
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> config) {
        // Default implementation - can be overridden
        return config != null;
    }
    
    @Override
    public TriggerEvent processEvent(Map<String, Object> payload, Map<String, String> metadata) {
        return TriggerEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .triggerType(getType())
                .payload(payload != null ? payload : new HashMap<>())
                .metadata(metadata != null ? metadata : new HashMap<>())
                .timestamp(Instant.now())
                .createdAt(Instant.now())
                .build();
    }
    
    @Override
    public boolean isHealthy() {
        return initialized && healthy;
    }
    
    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("type", getType());
        status.put("name", getName());
        status.put("initialized", initialized);
        status.put("healthy", healthy);
        status.put("activeWorkflows", activeWorkflows.size());
        status.put("workflowIds", activeWorkflows.keySet());
        return status;
    }
    
    @Override
    public void destroy() {
        log.info("Destroying trigger plugin: {}", getType());
        activeWorkflows.clear();
        initialized = false;
    }
    
    protected void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }
    
    protected boolean isWorkflowActive(String workflowId) {
        return activeWorkflows.containsKey(workflowId);
    }
    
    protected Map<String, Object> getWorkflowConfig(String workflowId) {
        return activeWorkflows.get(workflowId);
    }
}