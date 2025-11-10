package com.flowforge.trigger.service;

import com.flowforge.trigger.entity.TriggerRegistration;
import com.flowforge.trigger.event.TriggerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling email triggers.
 * Monitors email accounts and triggers workflows based on incoming emails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTriggerService {

    private final TriggerEventPublisher eventPublisher;
    private final JavaMailSender mailSender;

    /**
     * Sets up a new email trigger.
     * Validates email configuration.
     */
    public TriggerRegistration setupEmailTrigger(TriggerRegistration trigger) {
        log.info("Setting up email trigger for workflow: {}", trigger.getWorkflowId());

        // Validate email configuration
        Map<String, Object> config = trigger.getConfiguration();
        if (!config.containsKey("emailAddress")) {
            throw new IllegalArgumentException("Email trigger requires 'emailAddress' in configuration");
        }

        log.info("Email trigger configured for address: {}", config.get("emailAddress"));
        return trigger;
    }

    /**
     * Processes an incoming email that matches trigger criteria.
     * Called by the email polling service when a matching email is found.
     */
    public void processEmailTrigger(TriggerRegistration trigger, Message email) {
        log.info("Processing email trigger: triggerId={}", trigger.getId());

        if (!trigger.isEnabled()) {
            log.warn("Trigger is disabled, ignoring email: triggerId={}", trigger.getId());
            return;
        }

        try {
            // Extract email details
            Map<String, Object> emailData = extractEmailData(email);

            // Check if email matches trigger filters
            if (!matchesFilters(trigger.getConfiguration(), emailData)) {
                log.debug("Email does not match trigger filters, ignoring");
                return;
            }

            // Create trigger event
            TriggerEvent event = TriggerEvent.builder()
                    .eventId(UUID.randomUUID())
                    .triggerId(trigger.getId())
                    .workflowId(trigger.getWorkflowId())
                    .userId(trigger.getUserId())
                    .triggerType("email")
                    .timestamp(Instant.now())
                    .payload(emailData)
                    .metadata(buildEmailMetadata(email))
                    .build();

            // Publish to Kafka
            eventPublisher.publishTriggerEvent(event);

            log.info("Successfully processed email trigger: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Error processing email trigger: triggerId={}, error={}", 
                    trigger.getId(), e.getMessage(), e);
        }
    }

    /**
     * Extracts relevant data from an email message
     */
    private Map<String, Object> extractEmailData(Message email) throws Exception {
        Map<String, Object> data = new HashMap<>();
        
        data.put("subject", email.getSubject());
        data.put("from", InternetAddress.toString(email.getFrom()));
        data.put("receivedDate", email.getReceivedDate());
        
        // Extract email content
        Object content = email.getContent();
        if (content instanceof String) {
            data.put("body", content);
            data.put("contentType", "text");
        } else if (content instanceof Multipart) {
            data.put("body", extractTextFromMultipart((Multipart) content));
            data.put("contentType", "multipart");
        }
        
        return data;
    }

    /**
     * Extracts text content from multipart email
     */
    private String extractTextFromMultipart(Multipart multipart) throws Exception {
        StringBuilder text = new StringBuilder();
        
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                text.append(bodyPart.getContent().toString());
            }
        }
        
        return text.toString();
    }

    /**
     * Checks if email matches trigger filters
     */
    private boolean matchesFilters(Map<String, Object> config, Map<String, Object> emailData) {
        // Check subject filter
        if (config.containsKey("subjectContains")) {
            String filter = (String) config.get("subjectContains");
            String subject = (String) emailData.get("subject");
            if (subject == null || !subject.toLowerCase().contains(filter.toLowerCase())) {
                return false;
            }
        }

        // Check sender filter
        if (config.containsKey("fromAddress")) {
            String filter = (String) config.get("fromAddress");
            String from = (String) emailData.get("from");
            if (from == null || !from.toLowerCase().contains(filter.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Builds metadata for email trigger events
     */
    private Map<String, Object> buildEmailMetadata(Message email) throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "email");
        metadata.put("messageId", email.getHeader("Message-ID") != null ? 
                email.getHeader("Message-ID")[0] : null);
        metadata.put("receivedAt", Instant.now().toString());
        return metadata;
    }
}