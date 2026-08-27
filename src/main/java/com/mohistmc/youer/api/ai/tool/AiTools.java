package com.mohistmc.youer.api.ai.tool;

import com.mohistmc.youer.ai.tool.AiToolRegistry;
import org.bukkit.plugin.Plugin;

public final class AiTools {

    private AiTools() {
    }

    public static AiToolRegistration register(
            Plugin owner, AiToolDefinition definition, AiToolHandler handler) {
        return AiToolRegistry.registerActive(owner, definition, handler);
    }

    public static void unregister(Plugin owner) {
        AiToolRegistry.unregisterActive(owner);
    }
}
