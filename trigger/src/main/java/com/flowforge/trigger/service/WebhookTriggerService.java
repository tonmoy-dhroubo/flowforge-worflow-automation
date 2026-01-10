package com.flowforge.trigger.service;

import com.flowforge.trigger.dto.WebhookPayloadDto;
import com.flowforge.trigger.entity.TriggerRegistration;
import com.flowforge.trigger.event.TriggerEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Enumeration;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookTriggerService {

    private final TriggerEventPublisher eventPublisher;

    @Value("${webhook.base-url}")
    private String webhookBaseUrl;

    public TriggerRegistration setupWebhookTrigger(TriggerRegistration trigger) {
        log.info("Setting up webhook trigger for workflow: {}", trigger.getWorkflowId());

        String webhookToken = UUID.randomUUID().toString();

        String webhookPath = "/webhook/" + webhookToken;
        String fullWebhookUrl = webhookBaseUrl + webhookPath;

        trigger.setWebhookToken(webhookToken);
        trigger.setWebhookUrl(fullWebhookUrl);

        log.info("Created webhook URL: {}", fullWebhookUrl);
        return trigger;
    }

    public void processWebhookRequest(TriggerRegistration trigger, WebhookPayloadDto payload) {
        log.info("Processing webhook request for trigger: {}", trigger.getId());

        if (!trigger.isEnabled()) {
            log.warn("Trigger is disabled, ignoring webhook: triggerId={}", trigger.getId());
            return;
        }

        TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID())
                .triggerId(trigger.getId())
                .workflowId(trigger.getWorkflowId())
                .userId(trigger.getUserId())
                .triggerType("webhook")
                .timestamp(Instant.now())
                .payload(convertPayloadToMap(payload))
                .metadata(buildWebhookMetadata(payload))
                .build();

        eventPublisher.publishTriggerEvent(event);

        log.info("Successfully processed webhook trigger: eventId={}", event.getEventId());
    }

    private Map<String, Object> convertPayloadToMap(WebhookPayloadDto payload) {
        Map<String, Object> map = new HashMap<>();
        map.put("body", payload.getBody());
        map.put("headers", payload.getHeaders());
        map.put("queryParams", payload.getQueryParams());
        map.put("method", payload.getMethod());
        return map;
    }

    private Map<String, Object> buildWebhookMetadata(WebhookPayloadDto payload) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "webhook");
        metadata.put("remoteAddress", payload.getRemoteAddress());
        metadata.put("method", payload.getMethod());
        metadata.put("receivedAt", Instant.now().toString());
        return metadata;
    }

    public Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    public Map<String, String> extractQueryParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }
}
