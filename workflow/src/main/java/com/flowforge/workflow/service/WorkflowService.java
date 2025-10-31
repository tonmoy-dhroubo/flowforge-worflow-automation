package com.flowforge.workflow.service;

import com.flowforge.workflow.dto.WorkflowRequest;
import com.flowforge.workflow.dto.WorkflowResponse;
import com.flowforge.workflow.dto.WorkflowSummary;

import java.util.List;
import java.util.UUID;

public interface WorkflowService {
    WorkflowResponse createWorkflow(WorkflowRequest request, UUID userId);
    List<WorkflowSummary> getWorkflowsForUser(UUID userId);
    WorkflowResponse getWorkflowByIdAndUser(UUID workflowId, UUID userId);
    WorkflowResponse updateWorkflow(UUID workflowId, WorkflowRequest request, UUID userId);
    void deleteWorkflow(UUID workflowId, UUID userId);
}