package com.flowforge.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.workflow.dto.*;
import com.flowforge.workflow.entity.Workflow;
import com.flowforge.workflow.exception.WorkflowNotFoundException;
import com.flowforge.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    // We still need ObjectMapper for mapping between DTOs and Maps, but not for DB interaction
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public WorkflowResponse createWorkflow(WorkflowRequest request, UUID userId) {
        Workflow workflow = Workflow.builder()
                .name(request.name())
                .userId(userId)
                .enabled(request.enabled())
                // --- SIMPLIFIED LOGIC ---
                // No more toJson(). Just a simple assignment.
                .triggerDefinition(objectMapper.convertValue(request.trigger(),
                        new TypeReference<Map<String, Object>>() {}))
                .actionsDefinition(request.actions().stream()
                        .map(action -> objectMapper.convertValue(action,
                                new TypeReference<Map<String, Object>>() {}))
                        .collect(Collectors.toList()))
                .build();
        Workflow savedWorkflow = workflowRepository.save(workflow);
        return toWorkflowResponse(savedWorkflow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowSummary> getWorkflowsForUser(UUID userId) {
        return workflowRepository.findByUserId(userId).stream()
                .map(this::toWorkflowSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowByIdAndUser(UUID workflowId, UUID userId) {
        return workflowRepository.findByIdAndUserId(workflowId, userId)
                .map(this::toWorkflowResponse)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + workflowId));
    }

    @Transactional
    public WorkflowResponse updateWorkflow(UUID workflowId, WorkflowRequest request, UUID userId) {
        Workflow existingWorkflow = workflowRepository.findByIdAndUserId(workflowId, userId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + workflowId));

        existingWorkflow.setName(request.name());
        existingWorkflow.setEnabled(request.enabled());
        existingWorkflow.setTriggerDefinition(objectMapper.convertValue(request.trigger(), new TypeReference<Map<String, Object>>() {}));

        List<Map<String, Object>> actions = request.actions().stream()
                .map(action -> objectMapper.convertValue(action, new TypeReference<Map<String, Object>>() {}))
                .collect(Collectors.toList());
        existingWorkflow.setActionsDefinition(actions);

        Workflow updatedWorkflow = workflowRepository.save(existingWorkflow);
        return toWorkflowResponse(updatedWorkflow);
    }

    @Override
    @Transactional
    public void deleteWorkflow(UUID workflowId, UUID userId) {
        if (!workflowRepository.existsByIdAndUserId(workflowId, userId)) {
            throw new WorkflowNotFoundException("Workflow not found with ID: " + workflowId);
        }
        workflowRepository.deleteById(workflowId);
    }

    // --- HELPER METHODS ---
    // The old toJson() and fromJson() methods are GONE!
    // The mapping logic is now much cleaner.

    private WorkflowResponse toWorkflowResponse(Workflow workflow) {
        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getUserId(),
                workflow.isEnabled(),
                objectMapper.convertValue(workflow.getTriggerDefinition(), TriggerDto.class),
                workflow.getActionsDefinition().stream()
                        .map(actionMap -> objectMapper.convertValue(actionMap, ActionDto.class))
                        .collect(Collectors.toList()),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }

    private WorkflowSummary toWorkflowSummary(Workflow workflow) {
        return new WorkflowSummary(
                workflow.getId(),
                workflow.getName(),
                workflow.isEnabled(),
                workflow.getCreatedAt()
        );
    }
}