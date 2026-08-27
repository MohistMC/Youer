package com.mohistmc.youer.ai.tool;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public record AiToolOwner(String id, AiToolSource source, BooleanSupplier enabled) {

    public AiToolOwner {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Tool owner ID must not be blank");
        }
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(enabled, "enabled");
    }

    public boolean isEnabled() {
        return enabled.getAsBoolean();
    }
}
