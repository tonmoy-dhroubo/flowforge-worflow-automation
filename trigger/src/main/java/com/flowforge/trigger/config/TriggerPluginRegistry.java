package com.flowforge.trigger.config;

import com.flowforge.trigger.plugin.TriggerPlugin;
import com.flowforge.trigger.plugin.TriggerPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * Automatically registers all Spring-managed TriggerPlugin beans
 * with the TriggerPluginManager on application startup.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TriggerPluginRegistry {
    
    private final TriggerPluginManager pluginManager;
    private final List<TriggerPlugin> triggerPlugins;
    
    @EventListener(ContextRefreshedEvent.class)
    public void registerPlugins() {
        log.info("Registering {} built-in trigger plugins...", triggerPlugins.size());
        
        for (TriggerPlugin plugin : triggerPlugins) {
            pluginManager.registerPlugin(plugin);
        }
        
        log.info("Built-in trigger plugins registered successfully");
    }
}