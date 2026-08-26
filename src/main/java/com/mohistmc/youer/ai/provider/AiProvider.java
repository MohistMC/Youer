package com.mohistmc.youer.ai.provider;

import com.mohistmc.youer.ai.model.AiChatRequest;
import com.mohistmc.youer.ai.model.AiChatResponse;

public interface AiProvider {

    AiChatResponse chat(AiChatRequest request);
}
