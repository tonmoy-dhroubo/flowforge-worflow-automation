package com.flowforge.trigger.controller;

import com.flowforge.trigger.dto.TriggerRegistrationDto;
import com.flowforge.trigger.service.TriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/triggers")
@RequiredArgsConstructor
@Slf4j
public class TriggerManagementController {

    private final TriggerService triggerService;

    private static final String USER_ID_HEADER = "X-User-Id";

    @PostMapping
    public ResponseEntity<TriggerRegistrationDto> createTrigger(
            @RequestBody TriggerRegistrationDto request,
            @RequestHeader(USER_ID_HEADER) UUID userId) {

        log.info("Creating trigger: workflowId={}, type={}, userId={}", 
                request.getWorkflowId(), request.getTriggerType(), userId);

        request.setUserId(userId);
        TriggerRegistrationDto created = triggerService.createTrigger(request);

        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TriggerRegistrationDto>> getTriggersForUser(
            @RequestHeader(USER_ID_HEADER) UUID userId) {

        log.info("Fetching triggers for user: userId={}", userId);
        List<TriggerRegistrationDto> triggers = triggerService.getTriggersForUser(userId);

        return ResponseEntity.ok(triggers);
    }

    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<List<TriggerRegistrationDto>> getTriggersForWorkflow(
            @PathVariable UUID workflowId,
            @RequestHeader(USER_ID_HEADER) UUID userId) {

        log.info("Fetching triggers for workflow: workflowId={}, userId={}", workflowId, userId);
        List<TriggerRegistrationDto> triggers = triggerService.getTriggersForWorkflow(workflowId);

        List<TriggerRegistrationDto> userTriggers = triggers.stream()
                .filter(t -> t.getUserId().equals(userId))
                .toList();

        return ResponseEntity.ok(userTriggers);
    }

    @PutMapping("/{triggerId}")
    public ResponseEntity<TriggerRegistrationDto> updateTrigger(
            @PathVariable UUID triggerId,
            @RequestBody TriggerRegistrationDto request,
            @RequestHeader(USER_ID_HEADER) UUID userId) {

        log.info("Updating trigger: triggerId={}, userId={}", triggerId, userId);
        TriggerRegistrationDto updated = triggerService.updateTrigger(triggerId, request, userId);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{triggerId}")
    public ResponseEntity<Void> deleteTrigger(
            @PathVariable UUID triggerId,
            @RequestHeader(USER_ID_HEADER) UUID userId) {

        log.info("Deleting trigger: triggerId={}, userId={}", triggerId, userId);
        triggerService.deleteTrigger(triggerId, userId);

        return ResponseEntity.noContent().build();
    }
}
