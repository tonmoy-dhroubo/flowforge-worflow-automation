package com.flowforge.trigger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a registered trigger.
 * Stores configuration for webhook, scheduler, and email triggers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trigger_registrations", indexes = {
        @Index(name = "idx_workflow_id", columnList = "workflow_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_webhook_token", columnList = "webhook_token"),
        @Index(name = "idx_trigger_type", columnList = "trigger_type")
})
public class TriggerRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID workflowId;

    @Column(nullable = false)
    private UUID userId;

    /**
     * Type of trigger: webhook, scheduler, or email
     */
    @Column(nullable = false)
    private String triggerType;

    /**
     * Configuration specific to the trigger type.
     * For webhook: may contain validation rules, response templates
     * For scheduler: contains cron expression or interval
     * For email: contains email address, subject filter, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    /**
     * Whether this trigger is currently active
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * For webhook triggers: the unique URL path
     */
    @Column(unique = true)
    private String webhookUrl;

    /**
     * For webhook triggers: authentication token
     */
    @Column(unique = true)
    private String webhookToken;

    /**
     * Last time this trigger was fired
     */
    private Instant lastTriggeredAt;

    /**
     * For scheduler triggers: next scheduled execution time
     */
    private Instant nextScheduledAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}