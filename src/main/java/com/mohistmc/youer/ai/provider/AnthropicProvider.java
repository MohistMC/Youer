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
import com.mohistmc.youer.ai.model.AiContentPart;
import com.mohistmc.youer.ai.model.AiTextContent;
import com.mohistmc.youer.ai.model.AiToolCallContent;
import com.mohistmc.youer.ai.model.AiToolResultContent;
import com.mohistmc.youer.ai.model.AiRole;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public final class AnthropicProvider implements AiProvider {

    private final AiProfile profile;
    private final AiHttpClient httpClient;

    public AnthropicProvider(AiProfile profile, AiHttpClient httpClient) {
        this.profile = profile;
        this.httpClient = httpClient;
    }

    @Override
    public CompletionStage<AiChatResponse> chat(AiChatRequest request) {
        return ProviderSupport.execute(
                profile,
                httpClient,
                new AiHttpRequest(URI.create(profile.baseUrl()), headers(), requestBody(request).toString()))
                .thenApply(response -> {
                    ProviderSupport.requireSuccess(profile, response);
                    return parseResponse(response);
                });
    }

    @Override
    public AiProviderCapabilities capabilities() {
        return AiProviderCapabilities.TOOLS;
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
            if (item.role() == AiRole.SYSTEM) continue;
            Json blocks = Json.array();
            for (AiContentPart part : item.content()) {
                if (part instanceof AiTextContent(String text1)) {
                    blocks.add(Json.object().set("type", "text").set("text", text1));
                } else if (part instanceof AiToolCallContent(String id, String name, Json arguments)) {
                    blocks.add(Json.object().set("type", "tool_use").set("id", id)
                            .set("name", name).set("input", Json.read(arguments.toString())));
                } else if (part instanceof AiToolResultContent result) {
                    blocks.add(Json.object().set("type", "tool_result").set("tool_use_id", result.callId())
                            .set("content", result.content()).set("is_error", result.error()));
                }
            }
            String role = item.role() == AiRole.ASSISTANT ? "assistant" : "user";
            messages.add(Json.object().set("role", role).set("content", blocks));
        }
        Json body = Json.object()
                .set("model", profile.model())
                .set("messages", messages)
                .set("max_tokens", profile.maxTokens());
        String systemPrompt = ProviderSupport.systemPrompt(profile, request.messages());
        if (!systemPrompt.isBlank()) {
            body.set("system", systemPrompt);
        }
        if (!request.tools().isEmpty()) {
            Json tools = Json.array();
            request.tools().forEach(tool -> tools.add(Json.object().set("name", tool.name())
                    .set("description", tool.description())
                    .set("input_schema", Json.read(tool.inputSchema().toString()))));
            body.set("tools", tools);
        }
        return body;
    }

    private AiChatResponse parseResponse(AiHttpResponse response) {
        Json root = ProviderSupport.parseJson(profile, response);
        List<AiContentPart> content = new ArrayList<>();
        try {
            for (Json block : root.at("content").asJsonList()) {
                if ("text".equals(ProviderSupport.string(block, "type"))) {
                    String text = ProviderSupport.string(block, "text");
                    if (text != null) {
                        content.add(new AiTextContent(text));
                    }
                } else if ("tool_use".equals(ProviderSupport.string(block, "type"))) {
                    content.add(new AiToolCallContent(ProviderSupport.string(block, "id"),
                            ProviderSupport.string(block, "name"), block.at("input")));
                }
            }
        } catch (RuntimeException exception) {
            throw ProviderSupport.error(
                    profile, AiErrorType.INVALID_RESPONSE, response, "AI provider returned an invalid response shape");
        }
        if (content.isEmpty()) {
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
                new AiMessage(AiRole.ASSISTANT, content, Map.of()),
                ProviderSupport.string(root, "model"),
                ProviderSupport.string(root, "stop_reason"),
                usage);
    }
}
