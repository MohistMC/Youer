package com.mohistmc.youer.ai.tool.confirmation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class AiConfirmationStore {
    private final Clock clock;
    private final ConcurrentHashMap<UUID, AiPendingAction> pending = new ConcurrentHashMap<>();

    public AiConfirmationStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AiPendingAction create(UUID playerId, String summary, Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Confirmation timeout must be positive");
        }
        Instant created = clock.instant();
        AiPendingAction action = new AiPendingAction(UUID.randomUUID().toString().replace("-", ""),
                playerId, summary, created, created.plus(timeout), new CompletableFuture<>());
        AiPendingAction replaced = pending.put(playerId, action);
        if (replaced != null) replaced.future().complete(AiConfirmationDecision.REPLACED);
        CompletableFuture.delayedExecutor(Math.max(1L, timeout.toMillis()), TimeUnit.MILLISECONDS)
                .execute(() -> expire(action));
        return action;
    }

    public boolean confirm(UUID playerId, String id) {
        return complete(playerId, id, AiConfirmationDecision.APPROVED);
    }

    public boolean cancel(UUID playerId, String id) {
        return complete(playerId, id, AiConfirmationDecision.CANCELLED);
    }

    public void cancelAll() {
        for (AiPendingAction action : new ArrayList<>(pending.values())) {
            if (pending.remove(action.playerId(), action)) {
                action.future().complete(AiConfirmationDecision.CANCELLED);
            }
        }
    }

    private boolean complete(UUID playerId, String id, AiConfirmationDecision decision) {
        AiPendingAction action = pending.get(playerId);
        if (action == null || !action.id().equals(id) || !pending.remove(playerId, action)) return false;
        return action.future().complete(decision);
    }

    private void expire(AiPendingAction action) {
        if (pending.remove(action.playerId(), action)) {
            action.future().complete(AiConfirmationDecision.EXPIRED);
        }
    }
}
