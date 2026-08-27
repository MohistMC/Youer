package com.mohistmc.youer.ai.tool;

import java.util.logging.Logger;

public final class AiToolAudit {
    private final Logger logger;

    public AiToolAudit(Logger logger) {
        this.logger = logger;
    }

    public void record(String playerId, String tool, String outcome, long durationMillis) {
        try {
            logger.info(() -> "AI tool player=" + playerId + " tool=" + tool
                    + " outcome=" + outcome + " durationMs=" + durationMillis);
        } catch (RuntimeException ignored) {
            // Audit output must never change the result of a tool invocation.
        }
    }
}
