package com.mohistmc.youer.ai.model;

import java.util.Objects;

public record AiTextContent(String text) implements AiContentPart {

    public AiTextContent {
        Objects.requireNonNull(text, "text");
    }
}
