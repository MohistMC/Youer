package com.mohistmc.youer.ai.tool;

import com.mohistmc.mjson.Json;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AiToolSchemaValidator {

    private static final Set<String> COMMON = Set.of("type", "description", "enum");

    public void validateSchema(Json schema) {
        validateSchemaNode(schema, "$", true);
    }

    public List<String> validate(Json schema, Json arguments) {
        validateSchema(schema);
        List<String> errors = new ArrayList<>();
        validateValue(schema, arguments, "", errors);
        return List.copyOf(errors);
    }

    private static void validateSchemaNode(Json schema, String path, boolean root) {
        if (schema == null || !schema.isObject()) {
            throw new IllegalArgumentException(path + " schema must be an object");
        }
        String type = string(schema, "type");
        if (type == null) {
            throw new IllegalArgumentException(path + " schema type is required");
        }
        if (root && !"object".equals(type)) {
            throw new IllegalArgumentException("Root schema must be an object");
        }
        Set<String> allowed = new HashSet<>(COMMON);
        switch (type) {
            case "object" -> {
                allowed.addAll(Set.of("properties", "required", "additionalProperties"));
                Json properties = schema.has("properties") ? schema.at("properties") : Json.object();
                if (!properties.isObject()) {
                    throw new IllegalArgumentException(path + ".properties must be an object");
                }
                for (Map.Entry<String, Json> entry : properties.asJsonMap().entrySet()) {
                    validateSchemaNode(entry.getValue(), path + ".properties." + entry.getKey(), false);
                }
                if (schema.has("required")) {
                    if (!schema.at("required").isArray()) {
                        throw new IllegalArgumentException(path + ".required must be an array");
                    }
                    for (Json value : schema.at("required").asJsonList()) {
                        if (!value.isString() || !properties.has(value.asString())) {
                            throw new IllegalArgumentException(path + ".required contains an unknown property");
                        }
                    }
                }
                if (schema.has("additionalProperties") && !schema.at("additionalProperties").isBoolean()) {
                    throw new IllegalArgumentException(path + ".additionalProperties must be boolean");
                }
            }
            case "string" -> allowed.addAll(Set.of("minLength", "maxLength"));
            case "number", "integer" -> allowed.addAll(Set.of("minimum", "maximum"));
            case "boolean" -> {
            }
            case "array" -> {
                allowed.add("items");
                if (!schema.has("items")) {
                    throw new IllegalArgumentException(path + ".items is required");
                }
                validateSchemaNode(schema.at("items"), path + ".items", false);
            }
            default -> throw new IllegalArgumentException(path + " has unsupported type " + type);
        }
        for (String key : schema.asJsonMap().keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException(path + " has unsupported keyword " + key);
            }
        }
        requireNonNegativeInteger(schema, "minLength", path);
        requireNonNegativeInteger(schema, "maxLength", path);
        requireNumber(schema, "minimum", path);
        requireNumber(schema, "maximum", path);
        if (schema.has("enum") && !schema.at("enum").isArray()) {
            throw new IllegalArgumentException(path + ".enum must be an array");
        }
    }

    private static void validateValue(Json schema, Json value, String path, List<String> errors) {
        String label = path.isEmpty() ? "arguments" : path;
        String type = schema.at("type").asString();
        boolean correctType = switch (type) {
            case "object" -> value != null && value.isObject();
            case "string" -> value != null && value.isString();
            case "number" -> value != null && value.isNumber();
            case "integer" -> value != null && value.isNumber()
                    && value.asDouble() == Math.rint(value.asDouble());
            case "boolean" -> value != null && value.isBoolean();
            case "array" -> value != null && value.isArray();
            default -> false;
        };
        if (!correctType) {
            errors.add(label + " must be " + type);
            return;
        }
        if (schema.has("enum") && schema.at("enum").asJsonList().stream()
                .noneMatch(candidate -> candidate.equals(value))) {
            errors.add(label + " is not an allowed value");
        }
        switch (type) {
            case "object" -> validateObject(schema, value, path, errors);
            case "string" -> validateString(schema, value, label, errors);
            case "number", "integer" -> validateNumber(schema, value, label, errors);
            case "array" -> {
                int index = 0;
                for (Json item : value.asJsonList()) {
                    validateValue(schema.at("items"), item, label + "[" + index++ + "]", errors);
                }
            }
            default -> {
            }
        }
    }

    private static void validateObject(Json schema, Json value, String path, List<String> errors) {
        Json properties = schema.has("properties") ? schema.at("properties") : Json.object();
        if (schema.has("required")) {
            for (Json required : schema.at("required").asJsonList()) {
                String name = required.asString();
                if (!value.has(name)) {
                    errors.add(join(path, name) + " is required");
                }
            }
        }
        for (Map.Entry<String, Json> entry : value.asJsonMap().entrySet()) {
            if (properties.has(entry.getKey())) {
                validateValue(properties.at(entry.getKey()), entry.getValue(), join(path, entry.getKey()), errors);
            } else if (schema.has("additionalProperties") && !schema.at("additionalProperties").asBoolean()) {
                errors.add(join(path, entry.getKey()) + " is not allowed");
            }
        }
    }

    private static void validateString(Json schema, Json value, String path, List<String> errors) {
        int length = value.asString().length();
        if (schema.has("minLength") && length < schema.at("minLength").asInteger()) {
            errors.add(path + " is shorter than minLength");
        }
        if (schema.has("maxLength") && length > schema.at("maxLength").asInteger()) {
            errors.add(path + " is longer than maxLength");
        }
    }

    private static void validateNumber(Json schema, Json value, String path, List<String> errors) {
        double number = value.asDouble();
        if (schema.has("minimum") && number < schema.at("minimum").asDouble()) {
            errors.add(path + " is below minimum");
        }
        if (schema.has("maximum") && number > schema.at("maximum").asDouble()) {
            errors.add(path + " is above maximum");
        }
    }

    private static String join(String path, String name) {
        return path.isEmpty() ? name : path + "." + name;
    }

    private static String string(Json object, String field) {
        return object.has(field) && object.at(field).isString() ? object.at(field).asString() : null;
    }

    private static void requireNonNegativeInteger(Json schema, String field, String path) {
        if (schema.has(field) && (!schema.at(field).isNumber()
                || schema.at(field).asDouble() != Math.rint(schema.at(field).asDouble())
                || schema.at(field).asInteger() < 0)) {
            throw new IllegalArgumentException(path + "." + field + " must be a non-negative integer");
        }
    }

    private static void requireNumber(Json schema, String field, String path) {
        if (schema.has(field) && !schema.at(field).isNumber()) {
            throw new IllegalArgumentException(path + "." + field + " must be a number");
        }
    }
}
