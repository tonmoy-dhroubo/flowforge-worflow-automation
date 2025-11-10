package com.flowforge.trigger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO representing a trigger event that will be sent to Kafka.
 * This is the message format expected by the orchestrator service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerEventDto {
    
    /**
     * Unique identifier for this trigger event
     */
    private UUID eventId;
    
    /**
     * ID of the trigger registration that fired
     */
    private UUID triggerId;
    
    /**
     * ID of the workflow associated with this trigger
     */
    private UUID workflowId;
    
    /**
     * ID of the user who owns the workflow
     */
    private UUID userId;
    
    /**
     * Type of trigger (webhook, scheduler, email)
     */
    private String triggerType;
    
    /**
     * Timestamp when the trigger was fired
     */
    private Instant timestamp;
    
    /**
     * Payload data from the trigger (e.g., webhook body, email content)
     */
    private Map<String, Object> payload;
    
    /**
     * Additional metadata about the trigger event
     */
    private Map<String, Object> metadata;
}