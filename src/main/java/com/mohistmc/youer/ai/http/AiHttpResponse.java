package com.mohistmc.youer.ai.http;

import java.util.Map;

public record AiHttpResponse(int status, Map<String, String> headers, String body) {

    public AiHttpResponse {
        headers = Map.copyOf(headers);
    }

    public String header(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
