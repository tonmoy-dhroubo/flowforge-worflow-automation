package com.flowforge.workflow.dto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record WorkflowResponse(UUID id, String name, UUID userId, boolean enabled, TriggerDto trigger, List<ActionDto> actions, Instant createdAt, Instant updatedAt) {}