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
public class TriggerRegistrationDto {

    private UUID id;
    private UUID workflowId;
    private UUID userId;
    private String triggerType;
    private Map<String, Object> configuration;
    private boolean enabled;
    private String webhookUrl;
    private String webhookToken;
    private Instant createdAt;
    private Instant updatedAt;
}
