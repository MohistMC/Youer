package com.mohistmc.youer.ai.tool;

import com.mohistmc.youer.ai.model.AiMessage;
import com.mohistmc.youer.ai.provider.AiProvider;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import java.util.List;
import java.util.Objects;

public record AiAgentRequest(
        AiProvider provider,
        List<AiMessage> messages,
        AiToolRegistry.Snapshot tools,
        AiToolContext context,
        int maxSteps,
        int maxCallsPerTurn) {
    public AiAgentRequest {
        Objects.requireNonNull(provider, "provider");
        messages = List.copyOf(messages);
        Objects.requireNonNull(tools, "tools");
        Objects.requireNonNull(context, "context");
        if (maxSteps < 1 || maxCallsPerTurn < 1) throw new IllegalArgumentException("Agent limits must be positive");
    }
}
