package com.flowforge.trigger.plugin;

import com.flowforge.trigger.dto.TriggerEvent;

import java.util.Map;

/**
 * Base interface for all trigger plugins.
 * Implement this interface to create custom trigger types.
 */
public interface TriggerPlugin {
    
    /**
     * Unique identifier for this trigger plugin
     * Examples: "webhook", "schedule", "email", "slack", "github"
     */
    String getType();
    
    /**
     * Human-readable name for this trigger
     */
    String getName();
    
    /**
     * Description of what this trigger does
     */
    String getDescription();
    
    /**
     * Configuration schema for this trigger (JSON Schema format)
     * Defines what configuration fields are required
     */
    Map<String, Object> getConfigSchema();
    
    /**
     * Initialize the trigger with configuration
     * Called when the trigger is first loaded or when configuration changes
     * 
     * @param config Configuration map from workflow definition
     * @throws Exception if initialization fails
     */
    void initialize(Map<String, Object> config) throws Exception;
    
    /**
     * Start listening for trigger events
     * Called when a workflow with this trigger is enabled
     * 
     * @param workflowId The workflow ID this trigger is associated with
     * @param config The trigger configuration
     * @throws Exception if start fails
     */
    void start(String workflowId, Map<String, Object> config) throws Exception;
    
    /**
     * Stop listening for trigger events
     * Called when a workflow is disabled or deleted
     * 
     * @param workflowId The workflow ID to stop
     * @throws Exception if stop fails
     */
    void stop(String workflowId) throws Exception;
    
    /**
     * Validate the trigger configuration
     * 
     * @param config The configuration to validate
     * @return true if valid, false otherwise
     */
    boolean validateConfig(Map<String, Object> config);
    
    /**
     * Process an incoming trigger event
     * This method is called when an event occurs that should trigger workflows
     * 
     * @param payload The event payload
     * @param metadata Additional metadata about the event
     * @return TriggerEvent to be published to Kafka
     */
    TriggerEvent processEvent(Map<String, Object> payload, Map<String, String> metadata);
    
    /**
     * Check if the trigger is currently active and healthy
     */
    boolean isHealthy();
    
    /**
     * Get current status information about the trigger
     */
    Map<String, Object> getStatus();
    
    /**
     * Cleanup resources when the trigger is being unloaded
     */
    void destroy();
}