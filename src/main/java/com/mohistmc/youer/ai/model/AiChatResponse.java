package com.mohistmc.youer.ai.model;

public record AiChatResponse(String content, String model, String finishReason, AiTokenUsage usage) {

    public AiChatResponse {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("AI response content must not be blank");
        }
    }
}
