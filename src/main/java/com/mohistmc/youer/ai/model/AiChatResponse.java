package com.mohistmc.youer.ai.model;

import java.util.Objects;

public record AiChatResponse(AiMessage message, String model, String finishReason, AiTokenUsage usage) {

    public AiChatResponse {
        Objects.requireNonNull(message, "message");
        boolean hasText = message.content().stream()
                .filter(AiTextContent.class::isInstance)
                .map(AiTextContent.class::cast)
                .map(AiTextContent::text)
                .anyMatch(text -> !text.isBlank());
        boolean hasToolCall = message.content().stream().anyMatch(AiToolCallContent.class::isInstance);
        if (!hasText && !hasToolCall) {
            throw new IllegalArgumentException("AI response must contain text or a tool call");
        }
    }

    public AiChatResponse(String content, String model, String finishReason, AiTokenUsage usage) {
        this(new AiMessage(AiRole.ASSISTANT, content), model, finishReason, usage);
    }

    public String content() {
        return message.text();
    }
}
