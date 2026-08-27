package com.mohistmc.youer.ai.model;

import java.util.List;
import com.mohistmc.youer.api.ai.tool.AiToolDefinition;

public record AiChatRequest(List<AiMessage> messages, List<AiToolDefinition> tools) {

    public AiChatRequest {
        messages = List.copyOf(messages);
        tools = List.copyOf(tools);
    }

    public AiChatRequest(List<AiMessage> messages) {
        this(messages, List.of());
    }
}
