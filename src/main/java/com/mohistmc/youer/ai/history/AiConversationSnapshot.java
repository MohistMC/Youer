package com.mohistmc.youer.ai.history;

import com.mohistmc.youer.ai.model.AiMessage;
import java.util.List;

public record AiConversationSnapshot(long version, List<AiMessage> messages) {

    public AiConversationSnapshot {
        messages = List.copyOf(messages);
    }
}
