package com.flowforge.workflow.dto;
import java.util.Map;
public record TriggerDto(String type, Map<String, Object> config) {}