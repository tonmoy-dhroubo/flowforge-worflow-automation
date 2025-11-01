package com.flowforge.trigger.plugin.impl;

import com.flowforge.trigger.dto.TriggerEvent;
import com.flowforge.trigger.plugin.BaseTriggerPlugin;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Example: Email trigger plugin
 * This would monitor an email inbox and trigger workflows when emails arrive
 * 
 * To use as external plugin:
 * 1. Compile this class into a JAR
 * 2. Add MANIFEST.MF with: Trigger-Plugin-Class: com.flowforge.trigger.plugin.impl.EmailTriggerPlugin
 * 3. Copy JAR to ./plugins/triggers/ directory
 * 4. Plugin will be loaded automatically (or call /api/v1/plugins/reload)
 */
@Slf4j
public class EmailTriggerPlugin extends BaseTriggerPlugin {
    
    private Thread emailMonitorThread;
    private volatile boolean running = false;
    
    @Override
    public String getType() {
        return "email";
    }
    
    @Override
    public String getName() {
        return "Email Trigger";
    }
    
    @Override
    public String getDescription() {
        return "Triggers workflow when an email is received";
    }
    
    @Override
    public Map<String, Object> getConfigSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Email server config
        Map<String, Object> serverProp = new HashMap<>();
        serverProp.put("type", "string");
        serverProp.put("description", "IMAP server address");
        serverProp.put("example", "imap.gmail.com");
        properties.put("server", serverProp);
        
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("default", 993);
        properties.put("port", portProp);
        
        Map<String, Object> usernameProp = new HashMap<>();
        usernameProp.put("type", "string");
        usernameProp.put("description", "Email username");
        properties.put("username", usernameProp);
        
        Map<String, Object> passwordProp = new HashMap<>();
        passwordProp.put("type", "string");
        passwordProp.put("description", "Email password");
        passwordProp.put("format", "password");
        properties.put("password", passwordProp);
        
        Map<String, Object> folderProp = new HashMap<>();
        folderProp.put("type", "string");
        folderProp.put("default", "INBOX");
        properties.put("folder", folderProp);
        
        // Filter options
        Map<String, Object> subjectFilterProp = new HashMap<>();
        subjectFilterProp.put("type", "string");
        subjectFilterProp.put("description", "Filter by subject (regex)");
        properties.put("subjectFilter", subjectFilterProp);
        
        Map<String, Object> fromFilterProp = new HashMap<>();
        fromFilterProp.put("type", "string");
        fromFilterProp.put("description", "Filter by sender email");
        properties.put("fromFilter", fromFilterProp);
        
        schema.put("properties", properties);
        schema.put("required", new String[]{"server", "username", "password"});
        
        return schema;
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return false;
        }
        
        // Validate required fields
        String[] required = {"server", "username", "password"};
        for (String field : required) {
            if (!config.containsKey(field) || config.get(field) == null) {
                log.error("Email trigger missing required field: {}", field);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public void initialize(Map<String, Object> config) throws Exception {
        super.initialize(config);
        log.info("Email trigger plugin initialized");
    }
    
    @Override
    public void start(String workflowId, Map<String, Object> config) throws Exception {
        super.start(workflowId, config);
        
        // Start email monitoring if not already running
        if (!running) {
            startEmailMonitoring();
        }
        
        log.info("Email trigger started for workflow: {}", workflowId);
    }
    
    @Override
    public void stop(String workflowId) throws Exception {
        super.stop(workflowId);
        
        // Stop monitoring if no workflows are active
        if (activeWorkflows.isEmpty() && running) {
            stopEmailMonitoring();
        }
        
        log.info("Email trigger stopped for workflow: {}", workflowId);
    }
    
    private void startEmailMonitoring() {
        running = true;
        emailMonitorThread = new Thread(() -> {
            log.info("Email monitoring thread started");
            
            while (running) {
                try {
                    // Simulate email checking
                    // In real implementation, connect to IMAP server and check for new emails
                    
                    checkEmails();
                    
                    // Check every 30 seconds
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error monitoring emails", e);
                    setHealthy(false);
                }
            }
            
            log.info("Email monitoring thread stopped");
        });
        emailMonitorThread.setName("email-monitor");
        emailMonitorThread.setDaemon(true);
        emailMonitorThread.start();
    }
    
    private void stopEmailMonitoring() {
        running = false;
        if (emailMonitorThread != null) {
            emailMonitorThread.interrupt();
            try {
                emailMonitorThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void checkEmails() {
        // Simulate checking emails for all active workflows
        // In real implementation:
        // 1. Connect to IMAP server
        // 2. Check for new/unread emails
        // 3. Apply filters based on workflow config
        // 4. Create trigger events for matching emails
        
        log.debug("Checking emails for {} workflows", activeWorkflows.size());
        
        // Example: simulate finding an email
        if (Math.random() < 0.1) { // 10% chance for demo
            activeWorkflows.forEach((workflowId, config) -> {
                TriggerEvent event = TriggerEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .triggerType(getType())
                    .payload(Map.of(
                        "from", "sender@example.com",
                        "subject", "Test Email",
                        "body", "This is a test email body",
                        "receivedAt", Instant.now().toString()
                    ))
                    .metadata(Map.of(
                        "workflowId", workflowId,
                        "triggerSource", "email"
                    ))
                    .timestamp(Instant.now())
                    .build();
                
                // In real implementation, you'd call triggerService.processTrigger(event)
                log.info("Would trigger workflow {} for email", workflowId);
            });
        }
    }
    
    @Override
    public void destroy() {
        stopEmailMonitoring();
        super.destroy();
        log.info("Email trigger plugin destroyed");
    }
    
    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> status = super.getStatus();
        status.put("monitoring", running);
        status.put("monitorThread", emailMonitorThread != null ? 
            emailMonitorThread.getState().toString() : "NOT_STARTED");
        return status;
    }
}