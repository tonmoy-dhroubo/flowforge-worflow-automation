package com.flowforge.trigger.repository;

import com.flowforge.trigger.entity.TriggerRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing trigger registrations.
 */
@Repository
public interface TriggerRegistrationRepository extends JpaRepository<TriggerRegistration, UUID> {
    
    /**
     * Find all triggers for a specific user
     */
    List<TriggerRegistration> findByUserId(UUID userId);
    
    /**
     * Find all triggers for a specific workflow
     */
    List<TriggerRegistration> findByWorkflowId(UUID workflowId);
    
    /**
     * Find trigger by webhook token (for validating incoming webhook requests)
     */
    Optional<TriggerRegistration> findByWebhookToken(String webhookToken);
    
    /**
     * Find trigger by webhook URL
     */
    Optional<TriggerRegistration> findByWebhookUrl(String webhookUrl);
    
    /**
     * Find all enabled triggers of a specific type
     */
    List<TriggerRegistration> findByTriggerTypeAndEnabledTrue(String triggerType);
    
    /**
     * Find all scheduler triggers that are due for execution
     */
    @Query("SELECT t FROM TriggerRegistration t WHERE t.triggerType = 'scheduler' " +
           "AND t.enabled = true AND t.nextScheduledAt <= :currentTime")
    List<TriggerRegistration> findScheduledTriggersToExecute(Instant currentTime);
    
    /**
     * Find trigger by workflow ID and user ID
     */
    Optional<TriggerRegistration> findByWorkflowIdAndUserId(UUID workflowId, UUID userId);
}