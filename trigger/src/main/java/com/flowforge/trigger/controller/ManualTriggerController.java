package com.flowforge.trigger.controller;

import com.flowforge.trigger.dto.WebhookResponse;
import com.flowforge.trigger.service.SchedulerService;
import com.flowforge.trigger.service.TriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/triggers")
@RequiredArgsConstructor
@Slf4j
public class ManualTriggerController {

    private final TriggerService triggerService;
    private final SchedulerService schedulerService;

    /**
     * Manually trigger a workflow
     */
    @PostMapping("/manual/{workflowId}")
    public ResponseEntity<WebhookResponse> triggerWorkflowManually(
            @PathVariable String workflowId,
            @RequestBody(required = false) Map<String, Object> payload) {
        
        log.info("Manual trigger requested for workflow: {}", workflowId);
        
        String eventId = triggerService.createManualTrigger(workflowId, payload);
        
        WebhookResponse response = WebhookResponse.builder()
                .eventId(eventId)
                .status("accepted")
                .message("Manual trigger accepted for workflow: " + workflowId)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Manually trigger a schedule
     */
    @PostMapping("/schedule/{scheduleName}")
    public ResponseEntity<WebhookResponse> triggerScheduleManually(
            @PathVariable String scheduleName,
            @RequestBody(required = false) Map<String, Object> config) {
        
        log.info("Manual schedule trigger requested: {}", scheduleName);
        
        String eventId = schedulerService.triggerCustomSchedule(scheduleName, config);
        
        WebhookResponse response = WebhookResponse.builder()
                .eventId(eventId)
                .status("accepted")
                .message("Schedule trigger accepted: " + scheduleName)
                .build();
        
        return ResponseEntity.ok(response);
    }
}