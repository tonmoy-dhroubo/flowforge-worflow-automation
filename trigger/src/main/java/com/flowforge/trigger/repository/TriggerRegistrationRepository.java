package com.flowforge.trigger.repository;

import com.flowforge.trigger.entity.TriggerRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TriggerRegistrationRepository extends JpaRepository<TriggerRegistration, UUID> {
    
    List<TriggerRegistration> findByUserId(UUID userId);
    
    List<TriggerRegistration> findByWorkflowId(UUID workflowId);
    
    Optional<TriggerRegistration> findByWebhookToken(String webhookToken);
    
    Optional<TriggerRegistration> findByWebhookUrl(String webhookUrl);
    
    List<TriggerRegistration> findByTriggerTypeAndEnabledTrue(String triggerType);
    
    @Query("SELECT t FROM TriggerRegistration t WHERE t.triggerType = 'scheduler' " +
           "AND t.enabled = true AND t.nextScheduledAt <= :currentTime")
    List<TriggerRegistration> findScheduledTriggersToExecute(Instant currentTime);
    
    Optional<TriggerRegistration> findByWorkflowIdAndUserId(UUID workflowId, UUID userId);
}
