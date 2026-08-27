package com.mohistmc.youer.ai.error;

public class AiProviderException extends RuntimeException {

    private final AiErrorType type;
    private final String provider;
    private final Integer status;
    private final String requestId;

    public AiProviderException(AiErrorType type, String provider, Integer status, String requestId, String message) {
        super(message);
        this.type = type;
        this.provider = provider;
        this.status = status;
        this.requestId = requestId;
    }

    public AiErrorType type() {
        return type;
    }

    public String provider() {
        return provider;
    }

    public Integer status() {
        return status;
    }

    public String requestId() {
        return requestId;
    }
}
