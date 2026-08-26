package com.mohistmc.youer.ai.provider;

import com.mohistmc.mjson.Json;
import com.mohistmc.youer.ai.model.AiProfile;
import com.mohistmc.youer.ai.error.AiErrorType;
import com.mohistmc.youer.ai.error.AiProviderException;
import com.mohistmc.youer.ai.http.AiHttpClient;
import com.mohistmc.youer.ai.http.AiHttpRequest;
import com.mohistmc.youer.ai.http.AiHttpResponse;
import com.mohistmc.youer.ai.model.AiChatRequest;
import com.mohistmc.youer.ai.model.AiChatResponse;
import com.mohistmc.youer.ai.model.AiMessage;
import com.mohistmc.youer.ai.model.AiTokenUsage;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class OpenAiCompatibleProvider implements AiProvider {

    private final AiProfile profile;
    private final AiHttpClient httpClient;

    public OpenAiCompatibleProvider(AiProfile profile, AiHttpClient httpClient) {
        this.profile = profile;
        this.httpClient = httpClient;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        AiHttpResponse response = ProviderSupport.execute(
                profile,
                httpClient,
                new AiHttpRequest(URI.create(profile.baseUrl()), headers(), requestBody(request).toString()));
        if (response.status() < 200 || response.status() >= 300) {
            throw statusError(response);
        }
        return parseResponse(response);
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        if (profile.apiKey() != null && !profile.apiKey().isBlank()) {
            headers.put("Authorization", "Bearer " + profile.apiKey().trim());
        }
        return headers;
    }

    private Json requestBody(AiChatRequest request) {
        Json messages = Json.array();
        if (profile.systemPrompt() != null && !profile.systemPrompt().isBlank()) {
            messages.add(message("system", profile.systemPrompt()));
        }
        for (AiMessage item : request.messages()) {
            messages.add(message(item.role().name().toLowerCase(Locale.ROOT), item.content()));
        }
        return Json.object()
                .set("model", profile.model())
                .set("messages", messages)
                .set("max_tokens", profile.maxTokens())
                .set("stream", false);
    }

    private static Json message(String role, String content) {
        return Json.object().set("role", role).set("content", content);
    }

    private AiChatResponse parseResponse(AiHttpResponse response) {
        Json root = ProviderSupport.parseJson(profile, response);

        String content = null;
        String finishReason = null;
        try {
            for (Json choice : root.at("choices").asJsonList()) {
                Json message = choice.at("message");
                if (message.has("content") && message.at("content").isString()) {
                    String candidate = message.at("content").asString();
                    if (candidate != null && !candidate.isBlank()) {
                        content = candidate;
                        finishReason = nullableString(choice, "finish_reason");
                        break;
                    }
                }
            }
        } catch (RuntimeException exception) {
            throw error(AiErrorType.INVALID_RESPONSE, response, "AI provider returned an invalid response shape");
        }
        if (content == null) {
            throw error(AiErrorType.EMPTY_RESPONSE, response, "AI provider returned no text content");
        }

        AiTokenUsage usage = null;
        if (root.has("usage") && root.at("usage").isObject()) {
            Json value = root.at("usage");
            usage = new AiTokenUsage(
                    nullableInteger(value, "prompt_tokens"),
                    nullableInteger(value, "completion_tokens"),
                    nullableInteger(value, "total_tokens"));
        }
        return new AiChatResponse(content, nullableString(root, "model"), finishReason, usage);
    }

    private AiProviderException statusError(AiHttpResponse response) {
        AiErrorType type = switch (response.status()) {
            case 401, 403 -> AiErrorType.AUTHENTICATION;
            case 429 -> AiErrorType.RATE_LIMIT;
            default -> AiErrorType.HTTP;
        };
        return error(type, response, "AI provider request failed with HTTP " + response.status());
    }

    private AiProviderException error(AiErrorType type, AiHttpResponse response, String message) {
        return new AiProviderException(
                type,
                profile.name(),
                profile.provider(),
                response.status(),
                response.header("x-request-id"),
                message);
    }

    private static String nullableString(Json object, String field) {
        return object.has(field) && object.at(field).isString() ? object.at(field).asString() : null;
    }

    private static Integer nullableInteger(Json object, String field) {
        return object.has(field) && object.at(field).isNumber() ? object.at(field).asInteger() : null;
    }
}
