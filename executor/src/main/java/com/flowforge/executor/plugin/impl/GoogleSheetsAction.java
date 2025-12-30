package com.flowforge.executor.plugin.impl;
import com.flowforge.executor.plugin.ActionPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component @Slf4j
public class GoogleSheetsAction implements ActionPlugin {
    private final WebClient webClient = WebClient.builder().build();

    @Override public String getSupportedType() { return "GOOGLE_SHEET_ROW"; }
    @Override public Mono<Map<String, Object>> execute(Map<String, Object> config, Map<String, Object> context) {
        String spreadsheetId = stringValue(config, "spreadsheetId");
        String range = stringValue(config, "range");
        String apiKey = stringValue(config, "apiKey");

        if (spreadsheetId == null || range == null || apiKey == null) {
            return Mono.error(new IllegalArgumentException("Google Sheets action requires spreadsheetId, range, and apiKey"));
        }

        List<List<Object>> values = coerceValues(config.get("values"));
        if (values.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Google Sheets action requires values"));
        }

        String valueInputOption = stringValue(config, "valueInputOption");
        if (valueInputOption == null || valueInputOption.isBlank()) {
            valueInputOption = "USER_ENTERED";
        }

        String uri = UriComponentsBuilder.fromUriString("https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}/values/{range}:append")
                .queryParam("valueInputOption", valueInputOption)
                .queryParam("key", apiKey)
                .buildAndExpand(spreadsheetId, range)
                .toUriString();

        Map<String, Object> body = new HashMap<>();
        body.put("values", values);

        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("updatedRange", response.get("updates") instanceof Map ? ((Map<?, ?>) response.get("updates")).get("updatedRange") : null);
                    result.put("response", response);
                    return result;
                });
    }

    private List<List<Object>> coerceValues(Object raw) {
        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            if (!list.isEmpty() && list.get(0) instanceof List) {
                return (List<List<Object>>) raw;
            }
            List<List<Object>> wrapped = new ArrayList<>();
            wrapped.add(new ArrayList<>(list));
            return wrapped;
        }
        if (raw instanceof String) {
            List<List<Object>> wrapped = new ArrayList<>();
            List<Object> row = new ArrayList<>();
            row.add(raw);
            wrapped.add(row);
            return wrapped;
        }
        return new ArrayList<>();
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
