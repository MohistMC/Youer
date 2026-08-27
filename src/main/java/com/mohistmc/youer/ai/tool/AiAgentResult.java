package com.mohistmc.youer.ai.tool;

import com.mohistmc.youer.ai.history.AiConversationTurn;
import com.mohistmc.youer.ai.model.AiChatResponse;

public record AiAgentResult(AiChatResponse response, AiConversationTurn turn) {
}
