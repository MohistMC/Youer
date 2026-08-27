package com.mohistmc.youer.ai.tool.http;

import com.mohistmc.mjson.Json;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import com.mohistmc.youer.api.ai.tool.AiToolHandler;
import com.mohistmc.youer.api.ai.tool.AiToolResult;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiHttpToolHandler implements AiToolHandler {
    private static final Pattern ENV = Pattern.compile("\\$\\{ENV:([A-Za-z_][A-Za-z0-9_]*)}");
    private final AiHttpToolDefinition definition;
    private final AiExternalHttpTransport transport;
    public AiHttpToolHandler(AiHttpToolDefinition definition, AiExternalHttpTransport transport) {
        this.definition = definition; this.transport = transport;
    }
    @Override public CompletionStage<AiToolResult> execute(AiToolContext context, Json arguments) {
        String url = definition.uri().toString();
        for (Map.Entry<String, String> entry : definition.path().entrySet()) {
            String replacement = encode(arguments.at(entry.getValue()).asString());
            url = url.replace("{" + entry.getKey() + "}", replacement)
                    .replace("%7B" + entry.getKey() + "%7D", replacement);
        }
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : definition.query().entrySet()) {
            query.append(query.isEmpty() && !url.contains("?") ? '?' : '&').append(encode(entry.getKey()))
                    .append('=').append(encode(arguments.at(entry.getValue()).asString()));
        }
        Json body = Json.object();
        definition.jsonBody().forEach((field, argument) -> body.set(field, arguments.at(argument)));
        String payload = definition.jsonBody().isEmpty() ? null : body.toString();
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        definition.headers().forEach((key, value) -> headers.put(key, resolveEnv(value)));
        AiExternalHttpRequest request = new AiExternalHttpRequest(definition.method(), URI.create(url + query),
                headers, payload, definition.timeout(), definition.maxResponseLength());
        return transport.execute(request).handle((response, failure) -> {
            if (failure != null) return AiToolResult.error("External tool request failed");
            if (response.status() < 200 || response.status() >= 300) {
                return AiToolResult.error("External tool returned HTTP " + response.status());
            }
            String responseBody = response.body() == null ? "" : response.body();
            if (responseBody.length() > definition.maxResponseLength()) {
                responseBody = responseBody.substring(0, definition.maxResponseLength());
            }
            return AiToolResult.success(responseBody);
        });
    }
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
    private static String resolveEnv(String value) {
        Matcher matcher = ENV.matcher(value); StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String secret = System.getenv(matcher.group(1));
            if (secret == null) throw new IllegalStateException("Required HTTP tool environment variable is missing");
            matcher.appendReplacement(result, Matcher.quoteReplacement(secret));
        }
        matcher.appendTail(result); return result.toString();
    }
}
