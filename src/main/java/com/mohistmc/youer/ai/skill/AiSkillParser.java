package com.mohistmc.youer.ai.skill;

import com.mohistmc.youer.api.ai.tool.AiToolRisk;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class AiSkillParser {

    public static final int MAX_FILE_BYTES = 32 * 1024;
    private static final Set<String> FIELDS = Set.of(
            "id", "title", "description", "edition", "minecraft_version",
            "commands", "tools", "execution", "risk", "source");
    private static final List<String> REQUIRED_FIELDS = List.of(
            "id", "title", "description", "commands", "tools", "execution", "risk");
    private static final List<String> REQUIRED_SECTIONS = List.of(
            "Purpose", "Preconditions", "Procedure", "Validation", "Failure handling", "Examples");
    private static final Pattern ID = Pattern.compile(
            "(?:minecraft|youer|workflow|custom)\\.[a-z0-9][a-z0-9._-]{0,94}");
    private static final Pattern COMMAND = Pattern.compile("[a-z0-9][a-z0-9:_-]{0,127}");
    private static final Pattern TOOL = Pattern.compile("[a-z][a-z0-9_-]{0,63}");

    public AiSkill parse(String origin, String markdown, AiSkillSource source) {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("origin must not be blank");
        }
        if (markdown == null) {
            throw error(origin, "Skill content is required");
        }
        if (source == null) {
            throw error(origin, "Skill source is required");
        }
        if (markdown.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_BYTES) {
            throw error(origin, "Skill file exceeds the 32 KiB limit");
        }

        int firstLineEnd = markdown.indexOf('\n');
        if (firstLineEnd < 0 || !"---".equals(stripCarriageReturn(markdown.substring(0, firstLineEnd)))) {
            throw error(origin, "Missing opening front-matter delimiter");
        }

        int closingStart = findClosingDelimiter(markdown, firstLineEnd + 1);
        if (closingStart < 0) {
            throw error(origin, "Missing closing front-matter delimiter");
        }
        int closingEnd = markdown.indexOf('\n', closingStart);

        Map<String, String> metadata = new HashMap<>();
        int lineStart = firstLineEnd + 1;
        while (lineStart < closingStart) {
            int newline = markdown.indexOf('\n', lineStart);
            int lineEnd = newline < 0 || newline > closingStart ? closingStart : newline;
            String line = stripCarriageReturn(markdown.substring(lineStart, lineEnd));
            parseMetadataLine(origin, line, metadata);
            lineStart = newline + 1;
        }
        int bodyStart = closingEnd < 0 ? markdown.length() : closingEnd + 1;

        for (String field : REQUIRED_FIELDS) {
            if (!metadata.containsKey(field) || metadata.get(field).isBlank()) {
                throw error(origin, "Missing required field '" + field + "'");
            }
        }

        String id = metadata.get("id");
        validateId(origin, id, source);
        String title = scalar(origin, "title", metadata.get("title"));
        String description = scalar(origin, "description", metadata.get("description"));
        String edition = optionalScalar(origin, "edition", metadata.get("edition"));
        String minecraftVersion = optionalScalar(origin, "minecraft_version", metadata.get("minecraft_version"));
        String documentationSource = optionalScalar(origin, "source", metadata.get("source"));
        List<String> commands = list(origin, "commands", metadata.get("commands"), COMMAND);
        List<String> tools = list(origin, "tools", metadata.get("tools"), TOOL);
        if (commands.isEmpty() && tools.isEmpty()) {
            throw error(origin, "At least one command or tool is required");
        }
        AiSkillExecution execution = enumValue(
                origin, "execution", metadata.get("execution"), AiSkillExecution.class);
        AiToolRisk risk = enumValue(origin, "risk", metadata.get("risk"), AiToolRisk.class);

        if (source == AiSkillSource.BUILT_IN && id.startsWith("minecraft.")) {
            validateMinecraftMetadata(origin, edition, minecraftVersion, documentationSource);
        }

        String body = markdown.substring(bodyStart);
        validateBody(origin, body);
        return new AiSkill(id, title, description, edition, minecraftVersion, commands, tools,
                execution, risk, documentationSource, body, source, origin);
    }

    private static void parseMetadataLine(String origin, String line, Map<String, String> metadata) {
        if (line.isBlank()) {
            throw error(origin, "Blank lines are not allowed in front matter");
        }
        int separator = line.indexOf(':');
        if (separator <= 0) {
            throw error(origin, "Front matter must use 'key: value' syntax");
        }
        String key = line.substring(0, separator);
        if (!key.equals(key.trim()) || !FIELDS.contains(key)) {
            String label = key.trim();
            throw error(origin, FIELDS.contains(label)
                    ? "Invalid whitespace around field '" + label + "'"
                    : "Unsupported field '" + label + "'");
        }
        if (metadata.containsKey(key)) {
            throw error(origin, "Duplicate field '" + key + "'");
        }
        String value = line.substring(separator + 1).trim();
        rejectControlCharacters(origin, value);
        if (("commands".equals(key) || "tools".equals(key))
                && (!value.startsWith("[") || !value.endsWith("]"))) {
            throw error(origin, "Field '" + key + "' must use [comma, separated] syntax");
        }
        metadata.put(key, value);
    }

    private static int findClosingDelimiter(String markdown, int start) {
        int lineStart = start;
        while (lineStart <= markdown.length()) {
            int newline = markdown.indexOf('\n', lineStart);
            int lineEnd = newline < 0 ? markdown.length() : newline;
            if ("---".equals(stripCarriageReturn(markdown.substring(lineStart, lineEnd)))) {
                return lineStart;
            }
            if (newline < 0) {
                return -1;
            }
            lineStart = newline + 1;
        }
        return -1;
    }

    private static void validateId(String origin, String id, AiSkillSource source) {
        scalar(origin, "id", id);
        if (!ID.matcher(id).matches()) {
            throw error(origin, "Invalid skill id '" + id + "'");
        }
        if (source == AiSkillSource.CUSTOM && !id.startsWith("custom.")) {
            throw error(origin, "Custom Skills must use the custom.* namespace");
        }
        if (source == AiSkillSource.BUILT_IN && id.startsWith("custom.")) {
            throw error(origin, "Built-in Skills must use a reserved built-in namespace");
        }
    }

    private static void validateMinecraftMetadata(
            String origin, String edition, String minecraftVersion, String documentationSource) {
        if (!"java".equals(edition)) {
            throw error(origin, "Built-in Minecraft Skill edition must be java");
        }
        if (!"1.21.1".equals(minecraftVersion)) {
            throw error(origin, "Built-in Minecraft Skill minecraft_version must be 1.21.1");
        }
        if (documentationSource == null
                || !documentationSource.startsWith("https://minecraft.wiki/w/Commands/")) {
            throw error(origin, "Built-in Minecraft Skill source must be an HTTPS Minecraft Wiki command page");
        }
    }

    private static String scalar(String origin, String field, String value) {
        if (value == null || value.isBlank()) {
            throw error(origin, "Missing required field '" + field + "'");
        }
        if (startsYamlFeature(value)) {
            throw error(origin, "Field '" + field + "' contains unsupported YAML features");
        }
        rejectControlCharacters(origin, value);
        return value;
    }

    private static String optionalScalar(String origin, String field, String value) {
        return value == null ? null : scalar(origin, field, value);
    }

    private static List<String> list(String origin, String field, String value, Pattern itemPattern) {
        if (value == null || !value.startsWith("[") || !value.endsWith("]")) {
            throw error(origin, "Field '" + field + "' must use [comma, separated] syntax");
        }
        String content = value.substring(1, value.length() - 1).trim();
        if (content.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String candidate : content.split(",", -1)) {
            String item = candidate.trim();
            if (item.isEmpty() || !itemPattern.matcher(item).matches()) {
                throw error(origin, "Field '" + field + "' contains invalid value '" + item + "'");
            }
            if (!seen.add(item)) {
                throw error(origin, "Field '" + field + "' contains duplicate value '" + item + "'");
            }
            result.add(item);
        }
        return List.copyOf(result);
    }

    private static <E extends Enum<E>> E enumValue(
            String origin, String field, String value, Class<E> type) {
        scalar(origin, field, value);
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw error(origin, "Field '" + field + "' has unsupported value '" + value + "'");
        }
    }

    private static void validateBody(String origin, String body) {
        String normalized = body.replace("\r\n", "\n");
        int previous = -1;
        for (String section : REQUIRED_SECTIONS) {
            String heading = "# " + section;
            int index = normalized.indexOf(heading, previous + 1);
            if (index < 0 || (index > 0 && normalized.charAt(index - 1) != '\n')
                    || !isLineEnd(normalized, index + heading.length())) {
                throw error(origin, "Missing required section '" + heading + "'");
            }
            if (index < previous) {
                throw error(origin, "Required body sections are out of order");
            }
            previous = index;
        }
        for (int index = 0; index < REQUIRED_SECTIONS.size(); index++) {
            String heading = "# " + REQUIRED_SECTIONS.get(index);
            int start = normalized.indexOf('\n', normalized.indexOf(heading)) + 1;
            int end = index + 1 < REQUIRED_SECTIONS.size()
                    ? normalized.indexOf("# " + REQUIRED_SECTIONS.get(index + 1), start)
                    : normalized.length();
            if (start <= 0 || normalized.substring(start, end).isBlank()) {
                throw error(origin, "Required section '" + heading + "' must not be empty");
            }
        }
    }

    private static boolean isLineEnd(String value, int index) {
        return index == value.length() || value.charAt(index) == '\n' || value.charAt(index) == '\r';
    }

    private static boolean startsYamlFeature(String value) {
        char first = value.charAt(0);
        return first == '!' || first == '&' || first == '*' || first == '{'
                || first == '|' || first == '>' || first == '\'' || first == '"'
                || value.contains("${") || value.contains("{{") || value.contains("<<:");
    }

    private static void rejectControlCharacters(String origin, String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw error(origin, "Front matter contains a control character");
            }
        }
    }

    private static String stripCarriageReturn(String value) {
        return value.endsWith("\r") ? value.substring(0, value.length() - 1) : value;
    }

    private static AiSkillFormatException error(String origin, String message) {
        return new AiSkillFormatException(origin, message);
    }
}
