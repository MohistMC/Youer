package com.mohistmc.youer.ai.tool.http;

import com.google.gson.Gson;
import com.mohistmc.mjson.Json;
import com.mohistmc.youer.ai.tool.AiToolSchemaValidator;
import com.mohistmc.youer.api.ai.tool.AiToolDefinition;
import com.mohistmc.youer.api.ai.tool.AiToolExecutionMode;
import com.mohistmc.youer.api.ai.tool.AiToolRisk;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiHttpToolParser {
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final Pattern PATH_TOKEN = Pattern.compile("%7B([^%/]+)%7D", Pattern.CASE_INSENSITIVE);
    private final AiToolSchemaValidator validator = new AiToolSchemaValidator();
    private final Gson gson = new Gson();

    public List<AiHttpToolDefinition> parse(List<? extends Map<?, ?>> config) {
        List<AiHttpToolDefinition> result = new ArrayList<>();
        for (Map<?, ?> item : config) result.add(parseOne(item));
        return List.copyOf(result);
    }

    private AiHttpToolDefinition parseOne(Map<?, ?> item) {
        String name = required(item, "name");
        String method = required(item, "method").toUpperCase(java.util.Locale.ROOT);
        if (!METHODS.contains(method)) throw new IllegalArgumentException(name + ": unsupported HTTP method");
        String rawUrl = required(item, "url");
        URI uri = URI.create(rawUrl.replace("{", "%7B").replace("}", "%7D"));
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) throw new IllegalArgumentException(name + ": URL must be fixed HTTP(S)");
        AiToolRisk risk = AiToolRisk.valueOf(required(item, "risk").toUpperCase(java.util.Locale.ROOT));
        if (!"GET".equals(method) && risk == AiToolRisk.READ_ONLY) {
            throw new IllegalArgumentException(name + ": mutating HTTP method cannot be READ_ONLY");
        }
        Object schemaValue = item.get("input_schema");
        if (!(schemaValue instanceof Map<?, ?>)) throw new IllegalArgumentException(name + ": input_schema is required");
        Json schema = Json.read(gson.toJson(schemaValue));
        validator.validateSchema(schema);
        int timeout = integer(item, "timeout_seconds", 10);
        int max = integer(item, "max_response_length", 4096);
        if (timeout < 1 || max < 1 || max > 65_536) throw new IllegalArgumentException(name + ": invalid HTTP limits");
        AiToolDefinition tool = new AiToolDefinition(name, required(item, "description"), schema,
                required(item, "permission"), risk, AiToolExecutionMode.ASYNC, Duration.ofSeconds(timeout));
        Map<String, String> path = strings(item.get("path"));
        Map<String, String> query = strings(item.get("query"));
        Map<String, String> jsonBody = strings(item.get("json_body"));
        validateMappings(name, uri, schema, path, query, jsonBody);
        return new AiHttpToolDefinition(tool, method, uri, strings(item.get("headers")),
                path, query, jsonBody,
                Duration.ofSeconds(timeout), max);
    }

    private static void validateMappings(
            String name, URI uri, Json schema, Map<String, String> path,
            Map<String, String> query, Map<String, String> jsonBody) {
        Set<String> properties = schema.has("properties")
                ? schema.at("properties").asJsonMap().keySet() : Set.of();
        Set<String> mappedArguments = new HashSet<>();
        for (Map<String, String> mapping : List.of(path, query, jsonBody)) {
            for (String argument : mapping.values()) {
                if (!properties.contains(argument)) {
                    throw new IllegalArgumentException(name + ": mapping references undeclared argument " + argument);
                }
                if (!mappedArguments.add(argument)) {
                    throw new IllegalArgumentException(name + ": argument is mapped more than once: " + argument);
                }
            }
        }
        Matcher matcher = PATH_TOKEN.matcher(uri.toString());
        Set<String> tokens = new HashSet<>();
        while (matcher.find()) tokens.add(matcher.group(1));
        if (!tokens.equals(path.keySet())) {
            throw new IllegalArgumentException(name + ": path mappings must exactly match URL template tokens");
        }
    }

    private static String required(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException(key + " is required");
        return value.toString();
    }
    private static int integer(Map<?, ?> map, String key, int fallback) {
        Object value = map.get(key); return value instanceof Number number ? number.intValue() : fallback;
    }
    private static Map<String, String> strings(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(key.toString(), item.toString()));
        return result;
    }
}
