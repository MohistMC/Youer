package com.mohistmc.youer.ai.http;

import java.time.Duration;

@FunctionalInterface
public interface AiHttpClient {

    AiHttpResponse execute(AiHttpRequest request, Duration timeout);
}
