package com.flowforge.executor.service;
import com.flowforge.executor.dto.ExecutionResultDto;
import com.flowforge.executor.dto.ExecutionStartDto;
import com.flowforge.executor.plugin.PluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service @RequiredArgsConstructor @Slf4j
public class ActionExecutorService {
    private final PluginManager pluginManager;
    public Mono<ExecutionResultDto> executeAction(ExecutionStartDto startDto) {
        Map<String, Object> context = startDto.getContext();
        final Map<String, Object> resolvedContext = (context == null)
                ? defaultContext(startDto)
                : context;
        try {
            validateConfig(startDto.getActionType(), startDto.getActionConfig());
        } catch (IllegalArgumentException e) {
            return Mono.just(ExecutionResultDto.builder()
                    .executionId(startDto.getExecutionId())
                    .workflowId(startDto.getWorkflowId())
                    .stepIndex(startDto.getStepIndex())
                    .status("FAILURE")
                    .errorMessage(e.getMessage())
                    .build());
        }
        return Mono.justOrEmpty(pluginManager.getPlugin(startDto.getActionType()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Unknown action: " + startDto.getActionType())))
                .flatMap(plugin -> {
                    try { return plugin.execute(startDto.getActionConfig(), resolvedContext); }
                    catch (Exception e) { return Mono.error(e); }
                })
                .map(output -> ExecutionResultDto.builder()
                        .executionId(startDto.getExecutionId())
                        .workflowId(startDto.getWorkflowId())
                        .stepIndex(startDto.getStepIndex())
                        .status("SUCCESS")
                        .output(output).build())
                .onErrorResume(e -> {
                    log.error("Execution failed", e);
                    return Mono.just(ExecutionResultDto.builder()
                            .executionId(startDto.getExecutionId())
                            .workflowId(startDto.getWorkflowId())
                            .stepIndex(startDto.getStepIndex())
                            .status("FAILURE")
                            .errorMessage(e.getMessage()).build());
                });
    }

    private Map<String, Object> defaultContext(ExecutionStartDto startDto) {
        Map<String, Object> context = new HashMap<>();
        context.put("trigger", startDto.getTriggerPayload());
        return context;
    }

    private void validateConfig(String actionType, Map<String, Object> config) {
        if ("SLACK_MESSAGE".equalsIgnoreCase(actionType)) {
            requireKeys(config, "webhookUrl");
            return;
        }
        if ("GOOGLE_SHEET_ROW".equalsIgnoreCase(actionType)) {
            requireKeys(config, "spreadsheetId", "range", "apiKey", "values");
        }
    }

    private void requireKeys(Map<String, Object> config, String... keys) {
        if (config == null) {
            throw new IllegalArgumentException("Action config is required");
        }
        for (String key : keys) {
            Object value = config.get(key);
            if (value == null || String.valueOf(value).isBlank()) {
                throw new IllegalArgumentException("Action config missing required field: " + key);
            }
        }
    }
}
