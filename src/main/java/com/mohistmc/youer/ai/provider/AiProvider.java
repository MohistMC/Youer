package com.mohistmc.youer.ai.provider;

import com.mohistmc.youer.ai.model.AiChatRequest;
import com.mohistmc.youer.ai.model.AiChatResponse;
import java.util.concurrent.CompletionStage;

public interface AiProvider {

    CompletionStage<AiChatResponse> chat(AiChatRequest request);

    default AiProviderCapabilities capabilities() {
        return AiProviderCapabilities.NONE;
    }
}
