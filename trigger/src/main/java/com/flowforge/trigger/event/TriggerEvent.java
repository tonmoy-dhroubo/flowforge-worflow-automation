package com.flowforge.trigger.event;

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

    private UUID eventId;
    private UUID triggerId;
    private UUID workflowId;
    private UUID userId;
    private String triggerType;
    private Instant timestamp;
    private Map<String, Object> payload;
    private Map<String, Object> metadata;
}
