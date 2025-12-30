package com.flowforge.executor.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExecutionStartDto {
    private UUID executionId;
    private UUID workflowId;
    private int stepIndex;
    private String actionType;
    private Map<String, Object> actionConfig;
    private Map<String, Object> triggerPayload;
    private Map<String, Object> context;
}
