package com.flowforge.workflow.controller;

import com.flowforge.workflow.dto.WorkflowRequest;
import com.flowforge.workflow.dto.WorkflowResponse;
import com.flowforge.workflow.dto.WorkflowSummary;
import com.flowforge.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    // A constant for the header name provided by the API Gateway
    private static final String USER_ID_HEADER = "X-User-Id";

    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(@RequestBody WorkflowRequest request,
                                                           @RequestHeader(USER_ID_HEADER) UUID userId) {
        WorkflowResponse createdWorkflow = workflowService.createWorkflow(request, userId);
        return new ResponseEntity<>(createdWorkflow, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowSummary>> getWorkflowsForUser(@RequestHeader(USER_ID_HEADER) UUID userId) {
        return ResponseEntity.ok(workflowService.getWorkflowsForUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflowById(@PathVariable UUID id,
                                                            @RequestHeader(USER_ID_HEADER) UUID userId) {
        return ResponseEntity.ok(workflowService.getWorkflowByIdAndUser(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> updateWorkflow(@PathVariable UUID id,
                                                           @RequestBody WorkflowRequest request,
                                                           @RequestHeader(USER_ID_HEADER) UUID userId) {
        return ResponseEntity.ok(workflowService.updateWorkflow(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable UUID id,
                                               @RequestHeader(USER_ID_HEADER) UUID userId) {
        workflowService.deleteWorkflow(id, userId);
        return ResponseEntity.noContent().build();
    }
}