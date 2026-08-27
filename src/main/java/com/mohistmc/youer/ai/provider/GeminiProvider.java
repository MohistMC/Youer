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
import com.mohistmc.youer.ai.model.AiContentPart;
import com.mohistmc.youer.ai.model.AiTextContent;
import com.mohistmc.youer.ai.model.AiToolCallContent;
import com.mohistmc.youer.ai.model.AiToolResultContent;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    public AiProviderCapabilities capabilities() {
        return AiProviderCapabilities.TOOLS;
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
            Json parts = Json.array();
            for (AiContentPart part : item.content()) {
                if (part instanceof AiTextContent text) {
                    parts.add(Json.object().set("text", text.text()));
                } else if (part instanceof AiToolCallContent call) {
                    parts.add(Json.object().set("functionCall", Json.object().set("id", call.id())
                            .set("name", call.name()).set("args", Json.read(call.arguments().toString()))));
                } else if (part instanceof AiToolResultContent result) {
                    parts.add(Json.object().set("functionResponse", Json.object().set("id", result.callId())
                            .set("name", result.name()).set("response", Json.object()
                                    .set("content", result.content()).set("error", result.error()))));
                }
            }
            contents.add(Json.object().set("role", role).set("parts", parts));
        }
        Json body = Json.object()
                .set("contents", contents)
                .set("generationConfig", Json.object()
                        .set("maxOutputTokens", profile.maxTokens()));
        String systemPrompt = ProviderSupport.systemPrompt(profile, request.messages());
        if (!systemPrompt.isBlank()) {
            body.set("system_instruction", Json.object()
                    .set("parts", Json.array().add(Json.object().set("text", systemPrompt))));
        }
        if (!request.tools().isEmpty()) {
            Json declarations = Json.array();
            request.tools().forEach(tool -> declarations.add(Json.object().set("name", tool.name())
                    .set("description", tool.description())
                    .set("parameters", Json.read(tool.inputSchema().toString()))));
            body.set("tools", Json.array().add(Json.object().set("functionDeclarations", declarations)));
        }
        return body;
    }

    private AiChatResponse parseResponse(AiHttpResponse response) {
        Json root = ProviderSupport.parseJson(profile, response);
        List<AiContentPart> content = new ArrayList<>();
        String finishReason = null;
        try {
            for (Json candidate : root.at("candidates").asJsonList()) {
                for (Json part : candidate.at("content").at("parts").asJsonList()) {
                    String text = ProviderSupport.string(part, "text");
                    if (text != null) {
                        content.add(new AiTextContent(text));
                    } else if (part.has("functionCall") && part.at("functionCall").isObject()) {
                        Json call = part.at("functionCall");
                        content.add(new AiToolCallContent(ProviderSupport.string(call, "id"),
                                ProviderSupport.string(call, "name"), call.at("args")));
                    }
                }
                if (!content.isEmpty()) {
                    finishReason = ProviderSupport.string(candidate, "finishReason");
                    break;
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
        if (root.has("usageMetadata") && root.at("usageMetadata").isObject()) {
            Json value = root.at("usageMetadata");
            usage = new AiTokenUsage(
                    ProviderSupport.integer(value, "promptTokenCount"),
                    ProviderSupport.integer(value, "candidatesTokenCount"),
                    ProviderSupport.integer(value, "totalTokenCount"));
        }
        return new AiChatResponse(
                new AiMessage(AiRole.ASSISTANT, content, Map.of()),
                ProviderSupport.string(root, "modelVersion"), finishReason, usage);
    }
}
