package com.mohistmc.youer.ai.http;

import java.net.URI;
import java.util.Map;

public record AiHttpRequest(URI uri, Map<String, String> headers, String body) {

    public AiHttpRequest {
        headers = Map.copyOf(headers);
    }
}
