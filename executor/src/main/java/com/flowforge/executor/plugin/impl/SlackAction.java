package com.flowforge.executor.plugin.impl;
import com.flowforge.executor.plugin.ActionPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

@Component @Slf4j
public class SlackAction implements ActionPlugin {
    private final WebClient webClient = WebClient.builder().build();

    @Override public String getSupportedType() { return "SLACK_MESSAGE"; }
    @Override public Mono<Map<String, Object>> execute(Map<String, Object> config, Map<String, Object> context) {
        String webhookUrl = stringValue(config, "webhookUrl", "webhook_url");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return Mono.error(new IllegalArgumentException("Slack action requires webhookUrl"));
        }

        String message = stringValue(config, "message", "text");
        if (message == null || message.isBlank()) {
            message = String.valueOf(context);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("text", message);

        String channel = stringValue(config, "channel");
        if (channel != null && !channel.isBlank()) {
            payload.put("channel", channel);
        }

        return webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("statusCode", response.getStatusCode().value());
                    result.put("sent", response.getStatusCode().is2xxSuccessful());
                    return result;
                });
    }

    private String stringValue(Map<String, Object> config, String... keys) {
        if (config == null) {
            return null;
        }
        for (String key : keys) {
            Object value = config.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
