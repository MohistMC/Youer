package com.mohistmc.youer.ai;

import com.mohistmc.youer.ai.model.AiProfile;
import com.mohistmc.youer.ai.provider.AiProvider;

public record AiRuntime(
        boolean enabled,
        String command,
        String chatFormat,
        int maxHistory,
        int workerThreads,
        int queueCapacity,
        boolean toolsEnabled,
        int maxToolSteps,
        int maxToolCallsPerTurn,
        int confirmationTimeoutSeconds,
        boolean playerCommandsRequireConfirmation,
        AiProfile profile,
        AiProvider provider) {

    public AiRuntime(
            boolean enabled, String command,
            String chatFormat, int maxHistory, int workerThreads, int queueCapacity,
            AiProfile profile, AiProvider provider) {
        this(enabled, command, chatFormat, maxHistory, workerThreads, queueCapacity,
                false, 5, 8, 60, true, profile, provider);
    }
}
