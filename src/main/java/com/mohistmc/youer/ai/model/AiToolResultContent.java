package com.mohistmc.youer.ai.model;

import java.util.Objects;

public record AiToolResultContent(String callId, String name, String content, boolean error)
        implements AiContentPart {

    public AiToolResultContent {
        if (callId == null || callId.isBlank()) {
            throw new IllegalArgumentException("Tool result call ID must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool result name must not be blank");
        }
        Objects.requireNonNull(content, "content");
    }
}
