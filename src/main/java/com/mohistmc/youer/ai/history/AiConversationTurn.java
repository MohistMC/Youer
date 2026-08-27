package com.mohistmc.youer.ai.history;

import com.mohistmc.youer.ai.model.AiMessage;
import com.mohistmc.youer.ai.model.AiRole;
import java.util.List;

public record AiConversationTurn(List<AiMessage> messages) {
    public AiConversationTurn {
        messages = List.copyOf(messages);
        if (messages.size() < 2 || messages.getFirst().role() != AiRole.USER
                || messages.getLast().role() != AiRole.ASSISTANT
                || messages.getLast().text().isBlank()) {
            throw new IllegalArgumentException("Conversation turn must start with USER and end with text ASSISTANT");
        }
    }
}
