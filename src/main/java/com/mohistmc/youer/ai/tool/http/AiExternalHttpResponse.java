package com.mohistmc.youer.ai.tool.http;

import java.util.Map;

public record AiExternalHttpResponse(int status, Map<String, String> headers, String body) {
    public AiExternalHttpResponse { headers = Map.copyOf(headers); }
}
