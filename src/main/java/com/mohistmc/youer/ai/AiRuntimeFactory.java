package com.mohistmc.youer.ai;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.ai.http.AiHttpClient;
import com.mohistmc.youer.ai.model.AiProfile;
import com.mohistmc.youer.ai.provider.AiProvider;
import com.mohistmc.youer.ai.provider.AiProviderFactory;
import java.time.Duration;

public final class AiRuntimeFactory {

    private AiRuntimeFactory() {
    }

    public static AiRuntime createFromConfig(AiHttpClient httpClient) {
        AiProfile profile = new AiProfile(
                YouerConfig.ai_provider,
                YouerConfig.ai_baseUrl,
                YouerConfig.ai_api_key,
                YouerConfig.ai_model,
                YouerConfig.ai_system_prompt,
                YouerConfig.ai_max_tokens,
                Duration.ofSeconds(YouerConfig.ai_timeout_seconds),
                YouerConfig.ai_api_version);
        AiProvider provider = AiProviderFactory.create(profile, httpClient);
        return new AiRuntime(
                YouerConfig.ai_enable,
                YouerConfig.ai_command,
                YouerConfig.ai_chat_format,
                YouerConfig.ai_max_history,
                YouerConfig.ai_worker_threads,
                YouerConfig.ai_queue_capacity,
                YouerConfig.ai_tools_enable,
                Math.max(1, YouerConfig.ai_tools_max_steps),
                Math.max(1, YouerConfig.ai_tools_max_calls_per_turn),
                Math.max(1, YouerConfig.ai_tools_confirmation_timeout_seconds),
                YouerConfig.ai_tools_player_commands_require_confirmation,
                profile,
                provider);
    }
}
