package com.mohistmc.youer.ai.tool.http;

import com.mohistmc.youer.api.ai.tool.AiToolDefinition;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record AiHttpToolDefinition(
        AiToolDefinition tool, String method, URI uri, Map<String, String> headers,
        Map<String, String> path, Map<String, String> query, Map<String, String> jsonBody,
        Duration timeout, int maxResponseLength) {
    public AiHttpToolDefinition {
        headers = Map.copyOf(headers); path = Map.copyOf(path); query = Map.copyOf(query);
        jsonBody = Map.copyOf(jsonBody);
    }
}
