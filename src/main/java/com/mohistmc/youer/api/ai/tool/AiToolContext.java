package com.mohistmc.youer.api.ai.tool;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record AiToolContext(UUID playerId, String playerName, Locale locale) {

    public AiToolContext {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(locale, "locale");
    }
}
