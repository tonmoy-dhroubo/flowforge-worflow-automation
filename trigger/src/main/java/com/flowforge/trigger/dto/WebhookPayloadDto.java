package com.flowforge.trigger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for webhook request payload.
 * Captures all data sent to a webhook endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayloadDto {
    
    /**
     * Request headers
     */
    private Map<String, String> headers;
    
    /**
     * Query parameters
     */
    private Map<String, String> queryParams;
    
    /**
     * Request body
     */
    private Map<String, Object> body;
    
    /**
     * HTTP method used (GET, POST, etc.)
     */
    private String method;
    
    /**
     * Remote IP address
     */
    private String remoteAddress;
}