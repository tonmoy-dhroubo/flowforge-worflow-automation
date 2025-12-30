package com.flowforge.executor.plugin;
import reactor.core.publisher.Mono;
import java.util.Map;

public interface ActionPlugin {
    String getSupportedType();
    Mono<Map<String, Object>> execute(Map<String, Object> config, Map<String, Object> context);
}
