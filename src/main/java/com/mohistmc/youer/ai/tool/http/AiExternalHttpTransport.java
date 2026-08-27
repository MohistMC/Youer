package com.mohistmc.youer.ai.tool.http;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface AiExternalHttpTransport {
    CompletionStage<AiExternalHttpResponse> execute(AiExternalHttpRequest request);
}
