package com.mohistmc.youer.ai.tool.command;

import java.util.List;

public record AiCommandDescriptor(
        String label, String namespace, List<String> aliases, String description, String usage) {
    public AiCommandDescriptor {
        aliases = List.copyOf(aliases);
    }
}
