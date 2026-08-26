package com.mohistmc.youer.ai.http;

public final class AiHttpException extends RuntimeException {

    private final boolean timeout;

    public AiHttpException(boolean timeout, Throwable cause) {
        super(timeout ? "AI HTTP request timed out" : "AI HTTP request failed", cause);
        this.timeout = timeout;
    }

    public boolean timeout() {
        return timeout;
    }
}
