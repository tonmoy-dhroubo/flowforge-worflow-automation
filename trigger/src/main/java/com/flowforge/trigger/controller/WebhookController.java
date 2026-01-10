package com.flowforge.trigger.controller;

import com.flowforge.trigger.dto.WebhookPayloadDto;
import com.flowforge.trigger.entity.TriggerRegistration;
import com.flowforge.trigger.repository.TriggerRegistrationRepository;
import com.flowforge.trigger.service.TriggerService;
import com.flowforge.trigger.service.WebhookTriggerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final TriggerRegistrationRepository triggerRepository;
    private final WebhookTriggerService webhookTriggerService;
    private final TriggerService triggerService;

    @PostMapping("/{webhookToken}")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String webhookToken,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {

        log.info("Received webhook request: token={}, method={}", 
                webhookToken, request.getMethod());

        try {
            TriggerRegistration trigger = triggerRepository.findByWebhookToken(webhookToken)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid webhook token"));

            WebhookPayloadDto payload = WebhookPayloadDto.builder()
                    .headers(webhookTriggerService.extractHeaders(request))
                    .queryParams(webhookTriggerService.extractQueryParams(request))
                    .body(body != null ? body : new HashMap<>())
                    .method(request.getMethod())
                    .remoteAddress(request.getRemoteAddr())
                    .build();

            webhookTriggerService.processWebhookRequest(trigger, payload);

            triggerService.markTriggerFired(trigger.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Webhook received and processing");
            response.put("triggerId", trigger.getId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid webhook request: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{webhookToken}")
    public ResponseEntity<Map<String, Object>> handleWebhookGet(
            @PathVariable String webhookToken,
            HttpServletRequest request) {

        log.info("Received webhook GET request: token={}", webhookToken);

        try {
            TriggerRegistration trigger = triggerRepository.findByWebhookToken(webhookToken)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid webhook token"));

            WebhookPayloadDto payload = WebhookPayloadDto.builder()
                    .headers(webhookTriggerService.extractHeaders(request))
                    .queryParams(webhookTriggerService.extractQueryParams(request))
                    .body(new HashMap<>())
                    .method("GET")
                    .remoteAddress(request.getRemoteAddr())
                    .build();

            webhookTriggerService.processWebhookRequest(trigger, payload);
            triggerService.markTriggerFired(trigger.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Webhook received");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing GET webhook: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
