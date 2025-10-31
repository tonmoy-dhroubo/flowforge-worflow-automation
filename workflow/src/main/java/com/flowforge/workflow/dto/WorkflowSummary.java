package com.flowforge.workflow.dto;
import java.time.Instant;
import java.util.UUID;
public record WorkflowSummary(UUID id, String name, boolean enabled, Instant createdAt) {}