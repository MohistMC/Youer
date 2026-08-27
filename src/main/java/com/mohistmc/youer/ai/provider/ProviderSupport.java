package com.mohistmc.youer.ai.provider;

import com.mohistmc.mjson.Json;
import com.mohistmc.youer.ai.model.AiProfile;
import com.mohistmc.youer.ai.error.AiErrorType;
import com.mohistmc.youer.ai.error.AiProviderException;
import com.mohistmc.youer.ai.http.AiHttpResponse;
import com.mohistmc.youer.ai.http.AiHttpClient;
import com.mohistmc.youer.ai.http.AiHttpException;
import com.mohistmc.youer.ai.http.AiHttpRequest;
import com.mohistmc.youer.ai.model.AiMessage;
import com.mohistmc.youer.ai.model.AiRole;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

final class ProviderSupport {

    private ProviderSupport() {
    }

    static CompletionStage<AiHttpResponse> execute(AiProfile profile, AiHttpClient client, AiHttpRequest request) {
        try {
            return client.execute(request, profile.timeout()).handle((response, failure) -> {
                if (failure != null) {
                    throw transportFailure(profile, failure);
                }
                return response;
            });
        } catch (AiHttpException exception) {
            return CompletableFuture.failedFuture(transportFailure(profile, exception));
        }
    }

    private static RuntimeException transportFailure(AiProfile profile, Throwable failure) {
        Throwable cause = unwrap(failure);
        if (cause instanceof AiHttpException exception) {
            return new AiProviderException(
                    exception.timeout() ? AiErrorType.TIMEOUT : AiErrorType.HTTP,
                    profile.provider(),
                    null,
                    null,
                    exception.timeout() ? "AI provider request timed out" : "AI provider transport failed");
        }
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new CompletionException(cause);
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    static Json parseJson(AiProfile profile, AiHttpResponse response) {
        if (response.body() == null || response.body().isBlank()) {
            throw error(profile, AiErrorType.EMPTY_RESPONSE, response, "AI provider returned an empty response");
        }
        try {
            return Json.read(response.body());
        } catch (RuntimeException exception) {
            throw error(profile, AiErrorType.INVALID_RESPONSE, response, "AI provider returned malformed JSON");
        }
    }

    static void requireSuccess(AiProfile profile, AiHttpResponse response) {
        if (response.status() >= 200 && response.status() < 300) {
            return;
        }
        AiErrorType type = switch (response.status()) {
            case 401, 403 -> AiErrorType.AUTHENTICATION;
            case 429 -> AiErrorType.RATE_LIMIT;
            default -> AiErrorType.HTTP;
        };
        throw error(profile, type, response, "AI provider request failed with HTTP " + response.status());
    }

    static AiProviderException error(AiProfile profile, AiErrorType type, AiHttpResponse response, String message) {
        String requestId = response.header("x-request-id");
        if (requestId == null) {
            requestId = response.header("request-id");
        }
        return new AiProviderException(
                type, profile.provider(), response.status(), requestId, message);
    }

    static String string(Json object, String field) {
        return object.has(field) && object.at(field).isString() ? object.at(field).asString() : null;
    }

    static Integer integer(Json object, String field) {
        return object.has(field) && object.at(field).isNumber() ? object.at(field).asInteger() : null;
    }

    static String systemPrompt(AiProfile profile, List<AiMessage> messages) {
        List<String> parts = new ArrayList<>();
        if (profile.systemPrompt() != null && !profile.systemPrompt().isBlank()) {
            parts.add(profile.systemPrompt());
        }
        messages.stream()
                .filter(message -> message.role() == AiRole.SYSTEM)
                .map(AiMessage::text)
                .filter(text -> text != null && !text.isBlank())
                .forEach(parts::add);
        return String.join("\n\n", parts);
    }
}
