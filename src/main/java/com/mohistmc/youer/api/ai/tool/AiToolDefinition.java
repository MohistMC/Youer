package com.mohistmc.youer.api.ai.tool;

import com.mohistmc.mjson.Json;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

public record AiToolDefinition(
        String name,
        String description,
        Json inputSchema,
        String permission,
        AiToolRisk risk,
        AiToolExecutionMode executionMode,
        Duration timeout) {

    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    public AiToolDefinition {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Tool name must match " + SAFE_NAME.pattern());
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Tool description must not be blank");
        }
        Objects.requireNonNull(inputSchema, "inputSchema");
        if (!inputSchema.isObject()) {
            throw new IllegalArgumentException("Tool input schema must be an object");
        }
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("Tool permission must not be blank");
        }
        Objects.requireNonNull(risk, "risk");
        Objects.requireNonNull(executionMode, "executionMode");
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Tool timeout must be positive");
        }
        inputSchema = Json.read(inputSchema.toString());
    }
}
