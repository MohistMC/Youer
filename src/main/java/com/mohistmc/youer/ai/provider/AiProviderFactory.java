package com.mohistmc.youer.ai.provider;

import com.mohistmc.youer.ai.model.AiProfile;
import com.mohistmc.youer.ai.error.AiErrorType;
import com.mohistmc.youer.ai.error.AiProviderException;
import com.mohistmc.youer.ai.http.AiHttpClient;
import java.util.Locale;
import java.util.Set;

/**
 * Creates protocol-family AI providers from validated profiles.
 */
public final class AiProviderFactory {

    private static final Set<String> OPEN_AI_COMPATIBLE = Set.of(
            "openai-compatible", "deepseek", "openai", "qwen", "kimi", "groq", "ollama", "vllm");

    private AiProviderFactory() {
    }

    public static AiProvider create(AiProfile profile, AiHttpClient httpClient) {
        String provider = profile.provider().toLowerCase(Locale.ROOT);
        if (OPEN_AI_COMPATIBLE.contains(provider)) {
            return new OpenAiCompatibleProvider(profile, httpClient);
        }
        if ("anthropic".equals(provider)) {
            return new AnthropicProvider(profile, httpClient);
        }
        if ("gemini".equals(provider)) {
            return new GeminiProvider(profile, httpClient);
        }
        throw new AiProviderException(
                AiErrorType.CONFIGURATION,
                profile.provider(),
                null,
                null,
                "Unsupported AI provider: " + profile.provider());
    }
}
