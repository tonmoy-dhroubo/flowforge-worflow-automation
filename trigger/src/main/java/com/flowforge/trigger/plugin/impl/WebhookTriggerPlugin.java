package com.flowforge.trigger.plugin.impl;

import com.flowforge.trigger.plugin.BaseTriggerPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Built-in webhook trigger plugin.
 * Handles HTTP webhook events.
 */
@Component
@Slf4j
public class WebhookTriggerPlugin extends BaseTriggerPlugin {
    
    @Override
    public String getType() {
        return "webhook";
    }
    
    @Override
    public String getName() {
        return "Webhook Trigger";
    }
    
    @Override
    public String getDescription() {
        return "Triggers workflow when an HTTP webhook is received";
    }
    
    @Override
    public Map<String, Object> getConfigSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Path property
        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "Webhook endpoint path");
        pathProp.put("example", "/webhooks/my-workflow");
        properties.put("path", pathProp);
        
        // Method property
        Map<String, Object> methodProp = new HashMap<>();
        methodProp.put("type", "string");
        methodProp.put("enum", new String[]{"POST", "GET", "PUT", "DELETE", "PATCH"});
        methodProp.put("default", "POST");
        properties.put("method", methodProp);
        
        // Authentication property
        Map<String, Object> authProp = new HashMap<>();
        authProp.put("type", "object");
        Map<String, Object> authProps = new HashMap<>();
        
        Map<String, Object> authType = new HashMap<>();
        authType.put("type", "string");
        authType.put("enum", new String[]{"none", "basic", "bearer", "api_key"});
        authProps.put("type", authType);
        
        Map<String, Object> authValue = new HashMap<>();
        authValue.put("type", "string");
        authValue.put("description", "Authentication token/key");
        authProps.put("value", authValue);
        
        authProp.put("properties", authProps);
        properties.put("authentication", authProp);
        
        schema.put("properties", properties);
        schema.put("required", new String[]{"path"});
        
        return schema;
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return false;
        }
        
        // Path is required
        if (!config.containsKey("path") || config.get("path") == null) {
            log.error("Webhook trigger requires 'path' configuration");
            return false;
        }
        
        return true;
    }
    
    @Override
    public void start(String workflowId, Map<String, Object> config) throws Exception {
        super.start(workflowId, config);
        log.info("Webhook trigger started for workflow: {} at path: {}", 
            workflowId, config.get("path"));
    }
    
    @Override
    public void stop(String workflowId) throws Exception {
        super.stop(workflowId);
        log.info("Webhook trigger stopped for workflow: {}", workflowId);
    }
    
    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> status = super.getStatus();
        status.put("endpoints", activeWorkflows.entrySet().stream()
            .map(entry -> Map.of(
                "workflowId", entry.getKey(),
                "path", entry.getValue().get("path"),
                "method", entry.getValue().getOrDefault("method", "POST")
            ))
            .toList());
        return status;
    }
}