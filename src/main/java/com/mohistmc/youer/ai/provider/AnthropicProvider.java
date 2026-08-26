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
import com.mohistmc.youer.ai.model.AiTokenUsage;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AnthropicProvider implements AiProvider {

    private final AiProfile profile;
    private final AiHttpClient httpClient;

    public AnthropicProvider(AiProfile profile, AiHttpClient httpClient) {
        this.profile = profile;
        this.httpClient = httpClient;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        AiHttpResponse response = ProviderSupport.execute(
                profile,
                httpClient,
                new AiHttpRequest(URI.create(profile.baseUrl()), headers(), requestBody(request).toString()));
        ProviderSupport.requireSuccess(profile, response);
        return parseResponse(response);
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("x-api-key", profile.apiKey());
        headers.put("anthropic-version", profile.apiVersion());
        return headers;
    }

    private Json requestBody(AiChatRequest request) {
        Json messages = Json.array();
        for (AiMessage item : request.messages()) {
            if (item.role() != com.mohistmc.youer.ai.model.AiRole.SYSTEM) {
                messages.add(Json.object()
                        .set("role", item.role().name().toLowerCase(Locale.ROOT))
                        .set("content", item.content()));
            }
        }
        Json body = Json.object()
                .set("model", profile.model())
                .set("messages", messages)
                .set("max_tokens", profile.maxTokens());
        if (profile.systemPrompt() != null && !profile.systemPrompt().isBlank()) {
            body.set("system", profile.systemPrompt());
        }
        return body;
    }

    private AiChatResponse parseResponse(AiHttpResponse response) {
        Json root = ProviderSupport.parseJson(profile, response);
        StringBuilder content = new StringBuilder();
        try {
            for (Json block : root.at("content").asJsonList()) {
                if ("text".equals(ProviderSupport.string(block, "type"))) {
                    String text = ProviderSupport.string(block, "text");
                    if (text != null) {
                        content.append(text);
                    }
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
        if (root.has("usage") && root.at("usage").isObject()) {
            Integer input = ProviderSupport.integer(root.at("usage"), "input_tokens");
            Integer output = ProviderSupport.integer(root.at("usage"), "output_tokens");
            usage = new AiTokenUsage(input, output, input != null && output != null ? input + output : null);
        }
        return new AiChatResponse(
                content.toString(),
                ProviderSupport.string(root, "model"),
                ProviderSupport.string(root, "stop_reason"),
                usage);
    }
}
