package com.flowforge.trigger.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * Manages trigger plugins - loading, registering, and lifecycle management.
 * Supports hot-reload of plugins without server restart.
 */
@Component
@Slf4j
public class TriggerPluginManager {
    
    private final Map<String, TriggerPlugin> plugins = new ConcurrentHashMap<>();
    private final Map<String, ClassLoader> pluginClassLoaders = new ConcurrentHashMap<>();
    private final String pluginsDirectory = System.getProperty("plugins.dir", "./plugins/triggers");
    
    @PostConstruct
    public void initialize() {
        log.info("Initializing TriggerPluginManager");
        log.info("Plugins directory: {}", pluginsDirectory);
        
        // Create plugins directory if it doesn't exist
        File pluginsDir = new File(pluginsDirectory);
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs();
            log.info("Created plugins directory: {}", pluginsDirectory);
        }
        
        // Scan and load built-in plugins first
        loadBuiltInPlugins();
        
        // Load external plugins
        scanAndLoadPlugins();
        
        log.info("Loaded {} trigger plugins", plugins.size());
    }
    
    /**
     * Load built-in plugins that are part of the application
     */
    private void loadBuiltInPlugins() {
        log.info("Loading built-in plugins...");
        // Built-in plugins will be auto-discovered via Spring component scan
    }
    
    /**
     * Register a plugin (called by Spring for @Component plugins or manually)
     */
    public void registerPlugin(TriggerPlugin plugin) {
        try {
            String type = plugin.getType();
            
            if (plugins.containsKey(type)) {
                log.warn("Plugin {} already registered, replacing...", type);
                unregisterPlugin(type);
            }
            
            plugin.initialize(new HashMap<>());
            plugins.put(type, plugin);
            
            log.info("Registered trigger plugin: {} ({})", plugin.getName(), type);
        } catch (Exception e) {
            log.error("Failed to register plugin: {}", plugin.getType(), e);
        }
    }
    
    /**
     * Unregister a plugin
     */
    public void unregisterPlugin(String type) {
        TriggerPlugin plugin = plugins.get(type);
        if (plugin != null) {
            try {
                plugin.destroy();
                plugins.remove(type);
                
                // Cleanup class loader
                ClassLoader classLoader = pluginClassLoaders.remove(type);
                if (classLoader instanceof URLClassLoader) {
                    ((URLClassLoader) classLoader).close();
                }
                
                log.info("Unregistered trigger plugin: {}", type);
            } catch (Exception e) {
                log.error("Error unregistering plugin: {}", type, e);
            }
        }
    }
    
    /**
     * Get a plugin by type
     */
    public Optional<TriggerPlugin> getPlugin(String type) {
        return Optional.ofNullable(plugins.get(type));
    }
    
    /**
     * Get all registered plugins
     */
    public Map<String, TriggerPlugin> getAllPlugins() {
        return new HashMap<>(plugins);
    }
    
    /**
     * Scan and load plugins from the plugins directory
     */
    public void scanAndLoadPlugins() {
        File pluginsDir = new File(pluginsDirectory);
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            log.warn("Plugins directory does not exist: {}", pluginsDirectory);
            return;
        }
        
        File[] jarFiles = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            log.info("No plugin JAR files found in: {}", pluginsDirectory);
            return;
        }
        
        log.info("Found {} plugin JAR files", jarFiles.length);
        
        for (File jarFile : jarFiles) {
            try {
                loadPluginFromJar(jarFile);
            } catch (Exception e) {
                log.error("Failed to load plugin from: {}", jarFile.getName(), e);
            }
        }
    }
    
    /**
     * Load a plugin from a JAR file
     */
    private void loadPluginFromJar(File jarFile) throws Exception {
        log.info("Loading plugin from: {}", jarFile.getName());
        
        // Create class loader for the plugin
        URL[] urls = {jarFile.toURI().toURL()};
        URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader());
        
        // Read plugin metadata from JAR
        try (JarFile jar = new JarFile(jarFile)) {
            var manifest = jar.getManifest();
            if (manifest == null) {
                log.warn("No manifest found in: {}", jarFile.getName());
                return;
            }
            
            String pluginClass = manifest.getMainAttributes().getValue("Trigger-Plugin-Class");
            if (pluginClass == null) {
                log.warn("No Trigger-Plugin-Class attribute in manifest: {}", jarFile.getName());
                return;
            }
            
            // Load and instantiate the plugin
            Class<?> clazz = classLoader.loadClass(pluginClass);
            if (!TriggerPlugin.class.isAssignableFrom(clazz)) {
                log.error("Class {} does not implement TriggerPlugin", pluginClass);
                return;
            }
            
            TriggerPlugin plugin = (TriggerPlugin) clazz.getDeclaredConstructor().newInstance();
            
            // Store class loader
            pluginClassLoaders.put(plugin.getType(), classLoader);
            
            // Register the plugin
            registerPlugin(plugin);
            
            log.info("Successfully loaded plugin: {} from {}", plugin.getName(), jarFile.getName());
        }
    }
    
    /**
     * Reload a specific plugin
     */
    public void reloadPlugin(String type) {
        log.info("Reloading plugin: {}", type);
        unregisterPlugin(type);
        scanAndLoadPlugins();
    }
    
    /**
     * Reload all plugins
     */
    public void reloadAllPlugins() {
        log.info("Reloading all plugins...");
        
        // Unregister all external plugins (keep built-in ones)
        List<String> externalPlugins = new ArrayList<>();
        plugins.forEach((type, plugin) -> {
            if (pluginClassLoaders.containsKey(type)) {
                externalPlugins.add(type);
            }
        });
        
        externalPlugins.forEach(this::unregisterPlugin);
        
        // Rescan and load
        scanAndLoadPlugins();
        
        log.info("Reloaded all plugins. Total plugins: {}", plugins.size());
    }
    
    /**
     * Start a trigger for a workflow
     */
    public void startTrigger(String type, String workflowId, Map<String, Object> config) throws Exception {
        TriggerPlugin plugin = plugins.get(type);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found: " + type);
        }
        plugin.start(workflowId, config);
    }
    
    /**
     * Stop a trigger for a workflow
     */
    public void stopTrigger(String type, String workflowId) throws Exception {
        TriggerPlugin plugin = plugins.get(type);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found: " + type);
        }
        plugin.stop(workflowId);
    }
    
    /**
     * Get status of all plugins
     */
    public Map<String, Object> getPluginsStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalPlugins", plugins.size());
        status.put("pluginsDirectory", pluginsDirectory);
        
        Map<String, Map<String, Object>> pluginStatuses = new HashMap<>();
        plugins.forEach((type, plugin) -> pluginStatuses.put(type, plugin.getStatus()));
        status.put("plugins", pluginStatuses);
        
        return status;
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up TriggerPluginManager...");
        plugins.values().forEach(TriggerPlugin::destroy);
        
        // Close all class loaders
        pluginClassLoaders.values().forEach(classLoader -> {
            if (classLoader instanceof URLClassLoader) {
                try {
                    ((URLClassLoader) classLoader).close();
                } catch (Exception e) {
                    log.error("Error closing class loader", e);
                }
            }
        });
        
        plugins.clear();
        pluginClassLoaders.clear();
    }
}