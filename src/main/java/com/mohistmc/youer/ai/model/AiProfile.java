package com.mohistmc.youer.ai.model;

import java.time.Duration;

public record AiProfile(
        String name,
        String provider,
        String baseUrl,
        String apiKey,
        String model,
        String systemPrompt,
        int maxTokens,
        Duration timeout,
        String apiVersion) {
}
