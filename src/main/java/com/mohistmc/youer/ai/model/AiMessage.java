package com.mohistmc.youer.ai.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record AiMessage(AiRole role, List<AiContentPart> content, Map<String, String> attributes) {

    public AiMessage {
        Objects.requireNonNull(role, "role");
        content = List.copyOf(content);
        attributes = Map.copyOf(attributes);
    }

    public AiMessage(AiRole role, String text) {
        this(role, List.of(new AiTextContent(text)), Map.of());
    }

    public String text() {
        return content.stream()
                .filter(AiTextContent.class::isInstance)
                .map(AiTextContent.class::cast)
                .map(AiTextContent::text)
                .collect(Collectors.joining());
    }
}
