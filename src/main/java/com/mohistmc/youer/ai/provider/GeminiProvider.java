package com.mohistmc.youer.ai.provider;

import com.mohistmc.mjson.Json;
import com.mohistmc.youer.ai.model.AiProfile;
import com.mohistmc.youer.ai.error.AiErrorType;
import com.mohistmc.youer.ai.http.AiHttpClient;
import com.mohistmc.youer.ai.http.AiHttpRequest;
import com.mohistmc.youer.ai.http.AiHttpResponse;
import com.mohistmc.youer.ai.model.AiChatRequest;
import com.mohistmc.youer.ai.model.AiChatResponse;
import com.mohistmc.youer.ai.model.AiMessage;
import com.mohistmc.youer.ai.model.AiRole;
import com.mohistmc.youer.ai.model.AiTokenUsage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GeminiProvider implements AiProvider {

    private final AiProfile profile;
    private final AiHttpClient httpClient;

    public GeminiProvider(AiProfile profile, AiHttpClient httpClient) {
        this.profile = profile;
        this.httpClient = httpClient;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String encodedModel = URLEncoder.encode(profile.model(), StandardCharsets.UTF_8).replace("+", "%20");
        URI uri = URI.create(profile.baseUrl().replace("{model}", encodedModel));
        AiHttpResponse response = ProviderSupport.execute(
                profile, httpClient, new AiHttpRequest(uri, headers(), requestBody(request).toString()));
        ProviderSupport.requireSuccess(profile, response);
        return parseResponse(response);
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("x-goog-api-key", profile.apiKey());
        return headers;
    }

    private Json requestBody(AiChatRequest request) {
        Json contents = Json.array();
        for (AiMessage item : request.messages()) {
            if (item.role() == AiRole.SYSTEM) {
                continue;
            }
            String role = item.role() == AiRole.ASSISTANT ? "model" : "user";
            contents.add(Json.object()
                    .set("role", role)
                    .set("parts", Json.array().add(Json.object().set("text", item.content()))));
        }
        Json body = Json.object()
                .set("contents", contents)
                .set("generationConfig", Json.object()
                        .set("maxOutputTokens", profile.maxTokens()));
        if (profile.systemPrompt() != null && !profile.systemPrompt().isBlank()) {
            body.set("system_instruction", Json.object()
                    .set("parts", Json.array().add(Json.object().set("text", profile.systemPrompt()))));
        }
        return body;
    }

    private AiChatResponse parseResponse(AiHttpResponse response) {
        Json root = ProviderSupport.parseJson(profile, response);
        StringBuilder content = new StringBuilder();
        String finishReason = null;
        try {
            for (Json candidate : root.at("candidates").asJsonList()) {
                for (Json part : candidate.at("content").at("parts").asJsonList()) {
                    String text = ProviderSupport.string(part, "text");
                    if (text != null) {
                        content.append(text);
                    }
                }
                if (!content.toString().isBlank()) {
                    finishReason = ProviderSupport.string(candidate, "finishReason");
                    break;
                }
            }
        } catch (RuntimeException exception) {
            throw ProviderSupport.error(
                    profile, AiErrorType.INVALID_RESPONSE, response, "AI provider returned an invalid response shape");
        }
        if (content.toString().isBlank()) {
            throw ProviderSupport.error(
                    profile, AiErrorType.EMPTY_RESPONSE, response, "AI provider returned no text content");
        }

        AiTokenUsage usage = null;
        if (root.has("usageMetadata") && root.at("usageMetadata").isObject()) {
            Json value = root.at("usageMetadata");
            usage = new AiTokenUsage(
                    ProviderSupport.integer(value, "promptTokenCount"),
                    ProviderSupport.integer(value, "candidatesTokenCount"),
                    ProviderSupport.integer(value, "totalTokenCount"));
        }
        return new AiChatResponse(
                content.toString(), ProviderSupport.string(root, "modelVersion"), finishReason, usage);
    }
}
