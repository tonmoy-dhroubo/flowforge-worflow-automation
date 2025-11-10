package com.flowforge.trigger.service;

import com.flowforge.trigger.dto.TriggerRegistrationDto;
import com.flowforge.trigger.entity.TriggerRegistration;
import com.flowforge.trigger.repository.TriggerRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main service for managing trigger registrations.
 * Handles CRUD operations for all trigger types.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TriggerService {

    private final TriggerRegistrationRepository triggerRepository;
    private final WebhookTriggerService webhookTriggerService;
    private final SchedulerTriggerService schedulerTriggerService;
    private final EmailTriggerService emailTriggerService;

    /**
     * Creates a new trigger registration.
     * Delegates to specific trigger services based on type.
     */
    @Transactional
    public TriggerRegistrationDto createTrigger(TriggerRegistrationDto dto) {
        log.info("Creating new trigger: workflowId={}, type={}", dto.getWorkflowId(), dto.getTriggerType());

        TriggerRegistration trigger = TriggerRegistration.builder()
                .workflowId(dto.getWorkflowId())
                .userId(dto.getUserId())
                .triggerType(dto.getTriggerType())
                .configuration(dto.getConfiguration())
                .enabled(dto.isEnabled())
                .build();

        // Delegate to specific trigger service for type-specific setup
        switch (dto.getTriggerType().toLowerCase()) {
            case "webhook":
                trigger = webhookTriggerService.setupWebhookTrigger(trigger);
                break;
            case "scheduler":
                trigger = schedulerTriggerService.setupSchedulerTrigger(trigger);
                break;
            case "email":
                trigger = emailTriggerService.setupEmailTrigger(trigger);
                break;
            default:
                throw new IllegalArgumentException("Unsupported trigger type: " + dto.getTriggerType());
        }

        TriggerRegistration saved = triggerRepository.save(trigger);
        log.info("Successfully created trigger: id={}", saved.getId());

        return toDto(saved);
    }

    /**
     * Retrieves all triggers for a user
     */
    @Transactional(readOnly = true)
    public List<TriggerRegistrationDto> getTriggersForUser(UUID userId) {
        log.info("Fetching triggers for user: userId={}", userId);
        return triggerRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all triggers for a workflow
     */
    @Transactional(readOnly = true)
    public List<TriggerRegistrationDto> getTriggersForWorkflow(UUID workflowId) {
        log.info("Fetching triggers for workflow: workflowId={}", workflowId);
        return triggerRepository.findByWorkflowId(workflowId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates a trigger registration
     */
    @Transactional
    public TriggerRegistrationDto updateTrigger(UUID triggerId, TriggerRegistrationDto dto, UUID userId) {
        log.info("Updating trigger: triggerId={}", triggerId);

        TriggerRegistration trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new IllegalArgumentException("Trigger not found: " + triggerId));

        // Verify ownership
        if (!trigger.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this trigger");
        }

        trigger.setConfiguration(dto.getConfiguration());
        trigger.setEnabled(dto.isEnabled());

        // Update type-specific fields if needed
        if ("scheduler".equals(trigger.getTriggerType())) {
            trigger = schedulerTriggerService.updateSchedulerTrigger(trigger);
        }

        TriggerRegistration updated = triggerRepository.save(trigger);
        log.info("Successfully updated trigger: id={}", updated.getId());

        return toDto(updated);
    }

    /**
     * Deletes a trigger registration
     */
    @Transactional
    public void deleteTrigger(UUID triggerId, UUID userId) {
        log.info("Deleting trigger: triggerId={}", triggerId);

        TriggerRegistration trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new IllegalArgumentException("Trigger not found: " + triggerId));

        // Verify ownership
        if (!trigger.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this trigger");
        }

        triggerRepository.delete(trigger);
        log.info("Successfully deleted trigger: id={}", triggerId);
    }

    /**
     * Marks a trigger as fired and updates last triggered time
     */
    @Transactional
    public void markTriggerFired(UUID triggerId) {
        triggerRepository.findById(triggerId).ifPresent(trigger -> {
            trigger.setLastTriggeredAt(Instant.now());
            triggerRepository.save(trigger);
        });
    }

    /**
     * Converts entity to DTO
     */
    private TriggerRegistrationDto toDto(TriggerRegistration trigger) {
        return TriggerRegistrationDto.builder()
                .id(trigger.getId())
                .workflowId(trigger.getWorkflowId())
                .userId(trigger.getUserId())
                .triggerType(trigger.getTriggerType())
                .configuration(trigger.getConfiguration())
                .enabled(trigger.isEnabled())
                .webhookUrl(trigger.getWebhookUrl())
                .webhookToken(trigger.getWebhookToken())
                .createdAt(trigger.getCreatedAt())
                .updatedAt(trigger.getUpdatedAt())
                .build();
    }
}