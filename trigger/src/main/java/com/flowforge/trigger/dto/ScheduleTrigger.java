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
public class ScheduleTrigger {
    private String triggerId;
    private String cronExpression;
    private Map<String, Object> config;
    private boolean enabled;
}