package com.flowforge.workflow.dto;
import java.util.Map;
public record ActionDto(String type, Map<String, Object> config) {}