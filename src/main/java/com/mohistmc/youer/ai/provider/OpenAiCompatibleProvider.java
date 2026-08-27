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
import com.mohistmc.youer.ai.model.AiContentPart;
import com.mohistmc.youer.ai.model.AiTextContent;
import com.mohistmc.youer.ai.model.AiToolCallContent;
import com.mohistmc.youer.ai.model.AiToolResultContent;
import com.mohistmc.youer.ai.model.AiRole;
import com.mohistmc.youer.api.ai.tool.AiToolDefinition;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public final class OpenAiCompatibleProvider implements AiProvider {

    private final AiProfile profile;
    private final AiHttpClient httpClient;

    public OpenAiCompatibleProvider(AiProfile profile, AiHttpClient httpClient) {
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
                    if (response.status() < 200 || response.status() >= 300) {
                        throw statusError(request, response);
                    }
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
        if (profile.apiKey() != null && !profile.apiKey().isBlank()) {
            headers.put("Authorization", "Bearer " + profile.apiKey().trim());
        }
        return headers;
    }

    private Json requestBody(AiChatRequest request) {
        Json messages = Json.array();
        String systemPrompt = ProviderSupport.systemPrompt(profile, request.messages());
        if (!systemPrompt.isBlank()) {
            messages.add(message("system", systemPrompt));
        }
        for (AiMessage item : request.messages()) {
            if (item.role() == AiRole.SYSTEM) continue;
            serializeMessage(messages, item);
        }
        Json body = Json.object()
                .set("model", profile.model())
                .set("messages", messages)
                .set("max_tokens", profile.maxTokens())
                .set("stream", false);
        if (!request.tools().isEmpty()) {
            Json tools = Json.array();
            request.tools().forEach(definition -> tools.add(tool(definition)));
            body.set("tools", tools).set("tool_choice", "auto");
        }
        return body;
    }

    private static void serializeMessage(Json messages, AiMessage item) {
        if (item.role() == AiRole.TOOL) {
            for (AiContentPart part : item.content()) {
                if (part instanceof AiToolResultContent result) {
                    messages.add(Json.object().set("role", "tool")
                            .set("tool_call_id", result.callId()).set("content", result.content()));
                }
            }
            return;
        }
        Json message = Json.object().set("role", item.role().name().toLowerCase(Locale.ROOT));
        if (!item.text().isEmpty()) {
            message.set("content", item.text());
        }
        Json calls = Json.array();
        for (AiContentPart part : item.content()) {
            if (part instanceof AiToolCallContent(String id, String name, Json arguments)) {
                calls.add(Json.object().set("id", id).set("type", "function")
                        .set("function", Json.object().set("name", name)
                                .set("arguments", arguments.toString())));
            }
        }
        if (!calls.asJsonList().isEmpty()) {
            message.set("tool_calls", calls);
        }
        String reasoning = item.attributes().get("reasoning_content");
        if (reasoning != null) {
            message.set("reasoning_content", reasoning);
        }
        messages.add(message);
    }

    private static Json tool(AiToolDefinition definition) {
        return Json.object().set("type", "function").set("function", Json.object()
                .set("name", definition.name()).set("description", definition.description())
                .set("parameters", Json.read(definition.inputSchema().toString())));
    }

    private static Json message(String role, String content) {
        return Json.object().set("role", role).set("content", content);
    }

    private AiChatResponse parseResponse(AiHttpResponse response) {
        Json root = ProviderSupport.parseJson(profile, response);

        List<AiContentPart> content = new ArrayList<>();
        Map<String, String> attributes = new LinkedHashMap<>();
        String finishReason = null;
        try {
            for (Json choice : root.at("choices").asJsonList()) {
                Json message = choice.at("message");
                if (message.has("content") && message.at("content").isString()) {
                    String candidate = message.at("content").asString();
                    if (candidate != null && !candidate.isBlank()) {
                        content.add(new AiTextContent(candidate));
                    }
                }
                if (message.has("tool_calls") && message.at("tool_calls").isArray()) {
                    for (Json value : message.at("tool_calls").asJsonList()) {
                        Json function = value.at("function");
                        String id = nullableString(value, "id");
                        String name = nullableString(function, "name");
                        String arguments = nullableString(function, "arguments");
                        Json parsed = arguments == null ? null : Json.read(arguments);
                        content.add(new AiToolCallContent(id, name, parsed));
                    }
                }
                String reasoning = nullableString(message, "reasoning_content");
                if (reasoning != null) {
                    attributes.put("reasoning_content", reasoning);
                }
                finishReason = nullableString(choice, "finish_reason");
                if (!content.isEmpty()) break;
            }
        } catch (RuntimeException exception) {
            throw error(AiErrorType.INVALID_RESPONSE, response, "AI provider returned an invalid response shape");
        }
        if (content.isEmpty()) {
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
        return new AiChatResponse(new AiMessage(AiRole.ASSISTANT, content, attributes),
                nullableString(root, "model"), finishReason, usage);
    }

    private AiProviderException statusError(AiChatRequest request, AiHttpResponse response) {
        if (!request.tools().isEmpty() && (response.status() == 400 || response.status() == 422)
                && explicitlyRejectsTools(response.body())) {
            throw new AiToolCapabilityException(
                    profile.provider(), response.status(), response.header("x-request-id"));
        }
        AiErrorType type = switch (response.status()) {
            case 401, 403 -> AiErrorType.AUTHENTICATION;
            case 429 -> AiErrorType.RATE_LIMIT;
            default -> AiErrorType.HTTP;
        };
        return error(type, response, "AI provider request failed with HTTP " + response.status());
    }

    private static boolean explicitlyRejectsTools(String body) {
        try {
            Json error = Json.read(body).at("error");
            String code = nullableString(error, "code");
            String parameter = nullableString(error, "param");
            return List.of("unsupported_parameter", "unknown_parameter", "unsupported_feature").contains(code)
                    && List.of("tools", "tool_choice", "functions", "function_call").contains(parameter);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private AiProviderException error(AiErrorType type, AiHttpResponse response, String message) {
        return new AiProviderException(
                type,
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
