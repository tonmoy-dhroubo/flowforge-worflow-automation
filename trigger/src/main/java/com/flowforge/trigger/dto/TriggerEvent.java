package com.flowforge.trigger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerEvent {
    private String eventId;
    private String triggerType;  // webhook, schedule, manual, etc.
    private Map<String, Object> payload;
    private Map<String, String> metadata;
    private Instant timestamp;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
}