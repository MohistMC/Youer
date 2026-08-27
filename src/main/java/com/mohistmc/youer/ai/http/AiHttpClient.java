package com.mohistmc.youer.ai.http;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface AiHttpClient {

    CompletionStage<AiHttpResponse> execute(AiHttpRequest request, Duration timeout);
}
