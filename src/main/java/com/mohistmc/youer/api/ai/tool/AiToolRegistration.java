package com.mohistmc.youer.api.ai.tool;

@FunctionalInterface
public interface AiToolRegistration extends AutoCloseable {

    @Override
    void close();
}
