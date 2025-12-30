package com.flowforge.orchestrator.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ExecutionStartDto {
    private UUID executionId;
    private UUID workflowId;
    private int stepIndex;
    private String actionType;
    private Map<String, Object> actionConfig;
    private Map<String, Object> triggerPayload; // Data from the original trigger
    private Map<String, Object> context; // Trigger data + prior step outputs
}
