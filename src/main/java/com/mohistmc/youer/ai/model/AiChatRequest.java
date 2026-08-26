package com.mohistmc.youer.ai.model;

import java.util.List;

public record AiChatRequest(List<AiMessage> messages) {

    public AiChatRequest {
        messages = List.copyOf(messages);
    }
}
