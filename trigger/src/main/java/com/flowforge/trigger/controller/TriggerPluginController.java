package com.flowforge.trigger.controller;

import com.flowforge.trigger.plugin.TriggerPlugin;
import com.flowforge.trigger.plugin.TriggerPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for managing trigger plugins
 */
@RestController
@RequestMapping("/api/v1/plugins")
@RequiredArgsConstructor
@Slf4j
public class TriggerPluginController {
    
    private final TriggerPluginManager pluginManager;
    
    /**
     * List all available trigger plugins
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listPlugins() {
        Map<String, TriggerPlugin> plugins = pluginManager.getAllPlugins();
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", plugins.size());
        response.put("plugins", plugins.values().stream()
            .map(plugin -> Map.of(
                "type", plugin.getType(),
                "name", plugin.getName(),
                "description", plugin.getDescription(),
                "healthy", plugin.isHealthy(),
                "configSchema", plugin.getConfigSchema()
            ))
            .collect(Collectors.toList()));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get details of a specific plugin
     */
    @GetMapping("/{type}")
    public ResponseEntity<Map<String, Object>> getPlugin(@PathVariable String type) {
        return pluginManager.getPlugin(type)
            .map(plugin -> {
                Map<String, Object> response = new HashMap<>();
                response.put("type", plugin.getType());
                response.put("name", plugin.getName());
                response.put("description", plugin.getDescription());
                response.put("configSchema", plugin.getConfigSchema());
                response.put("status", plugin.getStatus());
                response.put("healthy", plugin.isHealthy());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get plugin status
     */
    @GetMapping("/{type}/status")
    public ResponseEntity<Map<String, Object>> getPluginStatus(@PathVariable String type) {
        return pluginManager.getPlugin(type)
            .map(plugin -> ResponseEntity.ok(plugin.getStatus()))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all plugins status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAllPluginsStatus() {
        return ResponseEntity.ok(pluginManager.getPluginsStatus());
    }
    
    /**
     * Reload all plugins (hot-reload)
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadAllPlugins() {
        log.info("Reloading all trigger plugins...");
        
        try {
            pluginManager.reloadAllPlugins();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "All plugins reloaded successfully");
            response.put("pluginCount", pluginManager.getAllPlugins().size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reloading plugins", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to reload plugins: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Reload a specific plugin
     */
    @PostMapping("/{type}/reload")
    public ResponseEntity<Map<String, Object>> reloadPlugin(@PathVariable String type) {
        log.info("Reloading trigger plugin: {}", type);
        
        try {
            pluginManager.reloadPlugin(type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Plugin reloaded successfully");
            response.put("type", type);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reloading plugin: {}", type, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to reload plugin: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Start a trigger for a workflow
     */
    @PostMapping("/{type}/start")
    public ResponseEntity<Map<String, Object>> startTrigger(
            @PathVariable String type,
            @RequestParam String workflowId,
            @RequestBody Map<String, Object> config) {
        
        log.info("Starting trigger {} for workflow: {}", type, workflowId);
        
        try {
            pluginManager.startTrigger(type, workflowId, config);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Trigger started successfully");
            response.put("type", type);
            response.put("workflowId", workflowId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting trigger", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Stop a trigger for a workflow
     */
    @PostMapping("/{type}/stop")
    public ResponseEntity<Map<String, Object>> stopTrigger(
            @PathVariable String type,
            @RequestParam String workflowId) {
        
        log.info("Stopping trigger {} for workflow: {}", type, workflowId);
        
        try {
            pluginManager.stopTrigger(type, workflowId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Trigger stopped successfully");
            response.put("type", type);
            response.put("workflowId", workflowId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error stopping trigger", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Validate trigger configuration
     */
    @PostMapping("/{type}/validate")
    public ResponseEntity<Map<String, Object>> validateConfig(
            @PathVariable String type,
            @RequestBody Map<String, Object> config) {
        
        return pluginManager.getPlugin(type)
            .map(plugin -> {
                boolean valid = plugin.validateConfig(config);
                
                Map<String, Object> response = new HashMap<>();
                response.put("valid", valid);
                response.put("type", type);
                
                if (!valid) {
                    response.put("message", "Invalid configuration");
                }
                
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}