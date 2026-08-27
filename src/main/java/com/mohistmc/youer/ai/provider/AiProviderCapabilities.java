package com.mohistmc.youer.ai.provider;

public record AiProviderCapabilities(boolean toolCalling) {

    public static final AiProviderCapabilities NONE = new AiProviderCapabilities(false);
    public static final AiProviderCapabilities TOOLS = new AiProviderCapabilities(true);
}
