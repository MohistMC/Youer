package com.mohistmc.youer.ai.tool.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record AiExternalHttpRequest(
        String method, URI uri, Map<String, String> headers, String body,
        Duration timeout, int maxResponseLength) {
    public AiExternalHttpRequest { headers = Map.copyOf(headers); }
}
