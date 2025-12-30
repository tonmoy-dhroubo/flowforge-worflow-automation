package com.flowforge.trigger.scheduler;

import com.flowforge.trigger.entity.TriggerRegistration;
import com.flowforge.trigger.repository.TriggerRegistrationRepository;
import com.flowforge.trigger.service.EmailTriggerService;
import com.flowforge.trigger.service.TriggerService;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailPollingExecutor {

    private final TriggerRegistrationRepository triggerRepository;
    private final EmailTriggerService emailTriggerService;
    private final TriggerService triggerService;

    @Value("${email.polling.enabled:false}")
    private boolean pollingEnabled;

    @Value("${email.polling.imap.host:}")
    private String imapHost;

    @Value("${email.polling.imap.port:993}")
    private int imapPort;

    @Value("${email.polling.imap.protocol:imaps}")
    private String imapProtocol;

    @Scheduled(fixedDelayString = "${email.polling.check-interval:300000}")
    public void pollEmailTriggers() {
        if (!pollingEnabled) {
            return;
        }

        if (imapHost == null || imapHost.isBlank()) {
            log.warn("Email polling is enabled but IMAP host is not configured.");
            return;
        }

        List<TriggerRegistration> triggers = triggerRepository.findByTriggerTypeAndEnabledTrue("email");
        if (triggers.isEmpty()) {
            return;
        }

        for (TriggerRegistration trigger : triggers) {
            try {
                pollTrigger(trigger);
            } catch (Exception e) {
                log.error("Error polling email trigger: triggerId={}, error={}", trigger.getId(), e.getMessage(), e);
            }
        }
    }

    private void pollTrigger(TriggerRegistration trigger) throws Exception {
        Map<String, Object> config = trigger.getConfiguration();
        if (config == null) {
            log.warn("Email trigger has no configuration: triggerId={}", trigger.getId());
            return;
        }

        String username = stringValue(config, "username", "emailAddress");
        String password = stringValue(config, "password");
        String folderName = stringValue(config, "folder");
        if (folderName == null || folderName.isBlank()) {
            folderName = "INBOX";
        }

        if (username == null || password == null) {
            log.warn("Email trigger missing credentials: triggerId={}", trigger.getId());
            return;
        }

        Properties props = new Properties();
        props.put("mail.store.protocol", imapProtocol);
        props.put("mail." + imapProtocol + ".host", imapHost);
        props.put("mail." + imapProtocol + ".port", String.valueOf(imapPort));
        if ("imaps".equalsIgnoreCase(imapProtocol)) {
            props.put("mail." + imapProtocol + ".ssl.enable", "true");
        }

        Session session = Session.getInstance(props);
        Store store = session.getStore(imapProtocol);
        store.connect(imapHost, imapPort, username, password);

        Folder folder = null;
        try {
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            for (Message message : messages) {
                emailTriggerService.processEmailTrigger(trigger, message);
                message.setFlag(Flags.Flag.SEEN, true);
                triggerService.markTriggerFired(trigger.getId());
            }
        } finally {
            if (folder != null && folder.isOpen()) {
                folder.close(false);
            }
            store.close();
        }
    }

    private String stringValue(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            Object value = config.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
