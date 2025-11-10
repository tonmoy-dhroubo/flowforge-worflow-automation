package com.flowforge.trigger.service;

import com.flowforge.trigger.dto.TriggerEventDto;
import com.flowforge.trigger.event.TriggerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for publishing trigger events to Kafka.
 * This is the bridge between trigger services and the orchestrator.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TriggerEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.trigger-events}")
    private String triggerEventsTopic;

    /**
     * Publishes a trigger event to Kafka.
     * The orchestrator service will consume this event and start workflow execution.
     *
     * @param event The trigger event to publish
     */
    public void publishTriggerEvent(TriggerEvent event) {
        log.info("Publishing trigger event: eventId={}, triggerId={}, workflowId={}, type={}", 
                event.getEventId(), event.getTriggerId(), event.getWorkflowId(), event.getTriggerType());

        // Convert domain event to DTO
        TriggerEventDto dto = TriggerEventDto.builder()
                .eventId(event.getEventId())
                .triggerId(event.getTriggerId())
                .workflowId(event.getWorkflowId())
                .userId(event.getUserId())
                .triggerType(event.getTriggerType())
                .timestamp(event.getTimestamp())
                .payload(event.getPayload())
                .metadata(event.getMetadata())
                .build();

        // Use workflow ID as the key for partitioning
        String key = event.getWorkflowId().toString();

        // Send to Kafka asynchronously
        CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(triggerEventsTopic, key, dto);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published trigger event to Kafka: eventId={}, partition={}, offset={}", 
                        event.getEventId(), 
                        result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish trigger event to Kafka: eventId={}, error={}", 
                        event.getEventId(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * Publishes a trigger event synchronously (blocks until sent).
     * Use this for critical triggers where you need confirmation of delivery.
     *
     * @param event The trigger event to publish
     * @return true if published successfully, false otherwise
     */
    public boolean publishTriggerEventSync(TriggerEvent event) {
        try {
            log.info("Publishing trigger event synchronously: eventId={}", event.getEventId());

            TriggerEventDto dto = TriggerEventDto.builder()
                    .eventId(event.getEventId())
                    .triggerId(event.getTriggerId())
                    .workflowId(event.getWorkflowId())
                    .userId(event.getUserId())
                    .triggerType(event.getTriggerType())
                    .timestamp(event.getTimestamp())
                    .payload(event.getPayload())
                    .metadata(event.getMetadata())
                    .build();

            String key = event.getWorkflowId().toString();
            SendResult<String, Object> result = kafkaTemplate.send(triggerEventsTopic, key, dto).get();

            log.info("Successfully published trigger event synchronously: eventId={}, partition={}, offset={}", 
                    event.getEventId(), 
                    result.getRecordMetadata().partition(), 
                    result.getRecordMetadata().offset());
            
            return true;
        } catch (Exception e) {
            log.error("Failed to publish trigger event synchronously: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            return false;
        }
    }
}