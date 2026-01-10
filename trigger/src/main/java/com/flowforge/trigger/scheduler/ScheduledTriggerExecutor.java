package com.flowforge.trigger.scheduler;

import com.flowforge.trigger.entity.TriggerRegistration;
import com.flowforge.trigger.repository.TriggerRegistrationRepository;
import com.flowforge.trigger.service.SchedulerTriggerService;
import com.flowforge.trigger.service.TriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTriggerExecutor {

    private final TriggerRegistrationRepository triggerRepository;
    private final SchedulerTriggerService schedulerTriggerService;
    private final TriggerService triggerService;

    @Value("${scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Scheduled(fixedDelayString = "${scheduler.check-interval:60000}")
    public void checkAndExecuteScheduledTriggers() {
        if (!schedulerEnabled) {
            return;
        }

        log.debug("Checking for scheduled triggers to execute");

        try {
            Instant now = Instant.now();
            List<TriggerRegistration> triggersToExecute = 
                    triggerRepository.findScheduledTriggersToExecute(now);

            if (triggersToExecute.isEmpty()) {
                log.debug("No scheduled triggers due for execution");
                return;
            }

            log.info("Found {} scheduled triggers to execute", triggersToExecute.size());

            for (TriggerRegistration trigger : triggersToExecute) {
                try {
                    schedulerTriggerService.processScheduledTrigger(trigger);

                    TriggerRegistration updated = schedulerTriggerService.updateSchedulerTrigger(trigger);
                    triggerRepository.save(updated);

                    triggerService.markTriggerFired(trigger.getId());

                    log.info("Successfully executed scheduled trigger: triggerId={}, nextRun={}", 
                            trigger.getId(), updated.getNextScheduledAt());

                } catch (Exception e) {
                    log.error("Error executing scheduled trigger: triggerId={}, error={}", 
                            trigger.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error in scheduled trigger check: {}", e.getMessage(), e);
        }
    }
}
