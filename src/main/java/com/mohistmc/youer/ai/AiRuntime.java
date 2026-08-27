package com.mohistmc.youer.ai;

import com.mohistmc.youer.ai.model.AiProfile;
import com.mohistmc.youer.ai.provider.AiProvider;
import java.util.Map;

public record AiRuntime(
        boolean enabled,
        String defaultProfileName,
        String command,
        String allCommand,
        String chatFormat,
        int maxHistory,
        int workerThreads,
        int queueCapacity,
        boolean toolsEnabled,
        int maxToolSteps,
        int maxToolCallsPerTurn,
        int confirmationTimeoutSeconds,
        boolean playerCommandsRequireConfirmation,
        Map<String, AiProfile> profiles,
        Map<String, AiProvider> providers) {

    public AiRuntime {
        profiles = Map.copyOf(profiles);
        providers = Map.copyOf(providers);
    }

    public AiRuntime(
            boolean enabled, String defaultProfileName, String command, String allCommand,
            String chatFormat, int maxHistory, int workerThreads, int queueCapacity,
            Map<String, AiProfile> profiles, Map<String, AiProvider> providers) {
        this(enabled, defaultProfileName, command, allCommand, chatFormat, maxHistory,
                workerThreads, queueCapacity, false, 5, 8, 60, true, profiles, providers);
    }

    public AiProfile defaultProfile() {
        return profiles.get(defaultProfileName);
    }

    public AiProvider defaultProvider() {
        return providers.get(defaultProfileName);
    }
}
