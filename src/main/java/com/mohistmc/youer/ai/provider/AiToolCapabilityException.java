package com.mohistmc.youer.ai.provider;

import com.mohistmc.youer.ai.error.AiErrorType;
import com.mohistmc.youer.ai.error.AiProviderException;

public final class AiToolCapabilityException extends AiProviderException {

    public AiToolCapabilityException(
            String profile, String provider, Integer status, String requestId) {
        super(AiErrorType.HTTP, profile, provider, status, requestId,
                "AI provider does not support tool calling");
    }
}
