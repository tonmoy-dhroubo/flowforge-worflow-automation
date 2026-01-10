package com.flowforge.orchestrator.service;

import com.flowforge.orchestrator.client.WorkflowServiceClient;
import com.flowforge.orchestrator.dto.*;
import com.flowforge.orchestrator.entity.ExecutionStatus;
import com.flowforge.orchestrator.entity.WorkflowExecution;
import com.flowforge.orchestrator.kafka.producer.ExecutionStartProducer;
import com.flowforge.orchestrator.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestrationService {

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowServiceClient workflowServiceClient;
    private final ExecutionStartProducer executionStartProducer;

    @Transactional
    public void startWorkflowExecution(TriggerEventDto triggerEvent) {
        UUID workflowId = triggerEvent.getWorkflowId();
        UUID userId = triggerEvent.getUserId();

        workflowServiceClient.getWorkflowById(workflowId, userId)
                .doOnError(WebClientResponseException.NotFound.class, e -> {
                    log.error("Workflow not found for id: {}. Cannot start execution.", workflowId);
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                .subscribe(workflowResponse -> {
                    if (!workflowResponse.isEnabled()) {
                        log.warn("Workflow {} is disabled. Skipping execution.", workflowId);
                        return;
                    }

                    WorkflowExecution execution = WorkflowExecution.builder()
                            .workflowId(workflowId)
                            .userId(userId)
                            .status(ExecutionStatus.PENDING)
                            .currentStep(0)
                            .triggerPayload(triggerEvent.getPayload())
                            .stepOutputs(new HashMap<>())
                            .build();

                    WorkflowExecution savedExecution = executionRepository.save(execution);
                    log.info("Created new workflow execution: id={}", savedExecution.getId());

                    executeStep(savedExecution, workflowResponse.getActions());
                });
    }

    @Transactional
    public void continueWorkflowExecution(ExecutionResultDto result) {
        WorkflowExecution execution = executionRepository.findById(result.getExecutionId())
                .orElseThrow(() -> new IllegalStateException("WorkflowExecution not found for id: " + result.getExecutionId()));

        if (!"SUCCESS".equalsIgnoreCase(result.getStatus())) {
            log.error("Execution step {} failed for executionId {}. Error: {}", result.getStepIndex(), result.getExecutionId(), result.getErrorMessage());
            execution.setStatus(ExecutionStatus.FAILED);
            executionRepository.save(execution);
            return;
        }

        execution.getStepOutputs().put("step_" + result.getStepIndex(), result.getOutput());

        execution.setCurrentStep(result.getStepIndex() + 1);
        executionRepository.save(execution);

        workflowServiceClient.getWorkflowById(execution.getWorkflowId(), execution.getUserId())
                .doOnError(WebClientResponseException.NotFound.class, e -> {
                    log.error("Workflow not found for id: {}. Halting execution for id: {}", execution.getWorkflowId(), execution.getId());
                    execution.setStatus(ExecutionStatus.FAILED);
                    executionRepository.save(execution);
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                .subscribe(workflowResponse -> executeStep(execution, workflowResponse.getActions()));
    }

    private void executeStep(WorkflowExecution execution, List<ActionDto> actions) {
        int currentStepIndex = execution.getCurrentStep();

        if (currentStepIndex >= actions.size()) {
            log.info("Workflow execution {} completed successfully.", execution.getId());
            execution.setStatus(ExecutionStatus.COMPLETED);
            executionRepository.save(execution);
            return;
        }

        execution.setStatus(ExecutionStatus.RUNNING);
        executionRepository.save(execution);

        ActionDto nextAction = actions.get(currentStepIndex);

        Map<String, Object> context = new HashMap<>();
        context.put("trigger", execution.getTriggerPayload());
        context.put("steps", execution.getStepOutputs());

        ExecutionStartDto startDto = ExecutionStartDto.builder()
                .executionId(execution.getId())
                .workflowId(execution.getWorkflowId())
                .stepIndex(currentStepIndex)
                .actionType(nextAction.getType())
                .actionConfig(nextAction.getConfig())
                .triggerPayload(execution.getTriggerPayload())
                .context(context)
                .build();

        executionStartProducer.sendExecutionStartEvent(startDto);
        log.info("Dispatched step {} for executionId {}.", currentStepIndex, execution.getId());
    }
}
