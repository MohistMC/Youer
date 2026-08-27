package com.mohistmc.youer.api.ai.tool;

import java.util.Objects;

public record AiToolResult(String content, boolean error) {

    public AiToolResult {
        Objects.requireNonNull(content, "content");
    }

    public static AiToolResult success(String content) {
        return new AiToolResult(content, false);
    }

    public static AiToolResult error(String content) {
        return new AiToolResult(content, true);
    }
}
