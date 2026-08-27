package com.mohistmc.youer.ai.tool;

import com.mohistmc.youer.api.ai.tool.AiToolDefinition;
import com.mohistmc.youer.api.ai.tool.AiToolHandler;
import java.util.Objects;

public record AiRegisteredTool(
        AiToolOwner owner, AiToolDefinition definition, AiToolHandler handler) {

    public AiRegisteredTool {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(handler, "handler");
    }
}
