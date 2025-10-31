package com.flowforge.workflow.dto;
import java.util.List;
public record WorkflowRequest(String name, boolean enabled, TriggerDto trigger, List<ActionDto> actions) {}