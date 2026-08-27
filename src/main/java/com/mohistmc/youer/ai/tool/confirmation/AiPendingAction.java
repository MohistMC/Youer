package com.mohistmc.youer.ai.tool.confirmation;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public record AiPendingAction(
        String id,
        UUID playerId,
        String summary,
        Instant createdAt,
        Instant expiresAt,
        CompletableFuture<AiConfirmationDecision> future) {

    public CompletionStage<AiConfirmationDecision> decision() {
        return future;
    }
}
