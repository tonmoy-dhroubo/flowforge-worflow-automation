package com.flowforge.trigger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayloadDto {

    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<String, Object> body;
    private String method;
    private String remoteAddress;
}
