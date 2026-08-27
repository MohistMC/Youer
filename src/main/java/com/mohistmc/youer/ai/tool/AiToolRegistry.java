package com.mohistmc.youer.ai.tool;

import com.mohistmc.youer.api.ai.tool.AiToolDefinition;
import com.mohistmc.youer.api.ai.tool.AiToolHandler;
import com.mohistmc.youer.api.ai.tool.AiToolRegistration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.bukkit.plugin.Plugin;

public final class AiToolRegistry {

    private static final AtomicReference<AiToolRegistry> ACTIVE = new AtomicReference<>();

    private final AiToolSchemaValidator schemaValidator;
    private final Map<String, AiRegisteredTool> pluginTools = new LinkedHashMap<>();
    private Map<String, AiRegisteredTool> runtimeTools = Map.of();

    public AiToolRegistry(AiToolSchemaValidator schemaValidator) {
        this.schemaValidator = Objects.requireNonNull(schemaValidator, "schemaValidator");
    }

    public static void activate(AiToolRegistry registry) {
        ACTIVE.set(Objects.requireNonNull(registry, "registry"));
    }

    public static void deactivate(AiToolRegistry registry) {
        ACTIVE.compareAndSet(registry, null);
    }

    public static AiToolRegistration registerActive(
            Plugin owner, AiToolDefinition definition, AiToolHandler handler) {
        AiToolRegistry registry = ACTIVE.get();
        if (registry == null) {
            throw new IllegalStateException("AI tool registry is unavailable");
        }
        AiToolPermissions.ensureOpDefault(definition.permission());
        return registry.register(owner, definition, handler);
    }

    public static void unregisterActive(Plugin owner) {
        AiToolRegistry registry = ACTIVE.get();
        if (registry != null) {
            registry.unregister(owner);
        }
    }

    public AiToolRegistration register(
            Plugin plugin, AiToolDefinition definition, AiToolHandler handler) {
        Objects.requireNonNull(plugin, "plugin");
        return register(new AiToolOwner(plugin.getName(), AiToolSource.PLUGIN, plugin::isEnabled),
                definition, handler);
    }

    synchronized AiToolRegistration register(
            AiToolOwner owner, AiToolDefinition definition, AiToolHandler handler) {
        AiRegisteredTool tool = registered(owner, definition, handler);
        String name = definition.name();
        if (pluginTools.containsKey(name) || runtimeTools.containsKey(name)) {
            throw new IllegalArgumentException("AI tool name is already registered: " + name);
        }
        pluginTools.put(name, tool);
        return () -> remove(name, tool);
    }

    public synchronized AiRegisteredTool registered(
            AiToolOwner owner, AiToolDefinition definition, AiToolHandler handler) {
        schemaValidator.validateSchema(definition.inputSchema());
        return new AiRegisteredTool(owner, definition, handler);
    }

    public synchronized void unregister(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        pluginTools.entrySet().removeIf(entry -> entry.getValue().owner().source() == AiToolSource.PLUGIN
                && entry.getValue().owner().id().equals(plugin.getName()));
    }

    public synchronized void replaceRuntimeTools(AiToolOwner owner, List<AiRegisteredTool> tools) {
        Objects.requireNonNull(owner, "owner");
        LinkedHashMap<String, AiRegisteredTool> replacement = new LinkedHashMap<>();
        for (AiRegisteredTool tool : List.copyOf(tools)) {
            if (!tool.owner().id().equals(owner.id())) {
                throw new IllegalArgumentException("Runtime tool owner mismatch");
            }
            String name = tool.definition().name();
            if (pluginTools.containsKey(name) || replacement.putIfAbsent(name, tool) != null) {
                throw new IllegalArgumentException("AI tool name is already registered: " + name);
            }
        }
        runtimeTools = Map.copyOf(replacement);
    }

    public synchronized Snapshot snapshot(Predicate<String> permissionCheck) {
        Objects.requireNonNull(permissionCheck, "permissionCheck");
        LinkedHashMap<String, AiRegisteredTool> visible = new LinkedHashMap<>();
        addVisible(runtimeTools, permissionCheck, visible);
        addVisible(pluginTools, permissionCheck, visible);
        return new Snapshot(visible);
    }

    private static void addVisible(
            Map<String, AiRegisteredTool> source,
            Predicate<String> permissionCheck,
            Map<String, AiRegisteredTool> target) {
        source.forEach((name, tool) -> {
            if (tool.owner().isEnabled() && permissionCheck.test(tool.definition().permission())) {
                target.put(name, tool);
            }
        });
    }

    private synchronized void remove(String name, AiRegisteredTool expected) {
        pluginTools.remove(name, expected);
    }

    public static final class Snapshot {
        private final Map<String, AiRegisteredTool> tools;
        private final List<AiToolDefinition> definitions;

        private Snapshot(Map<String, AiRegisteredTool> tools) {
            this.tools = Map.copyOf(tools);
            this.definitions = List.copyOf(tools.values().stream()
                    .map(AiRegisteredTool::definition).toList());
        }

        public List<AiToolDefinition> definitions() {
            return definitions;
        }

        public AiRegisteredTool find(String name) {
            return tools.get(name);
        }

        public List<AiRegisteredTool> tools() {
            return List.copyOf(new ArrayList<>(tools.values()));
        }
    }
}
