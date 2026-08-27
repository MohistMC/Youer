package com.mohistmc.youer.ai.skill;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;

public final class AiSkillLoader {

    public static final int MAX_CUSTOM_FILES = 64;
    public static final int MAX_CUSTOM_TOTAL_BYTES = 512 * 1024;
    private static final Pattern RESOURCE_PATH = Pattern.compile("ai/skills/[a-z0-9/_-]+\\.md");

    private final AiSkillParser parser;
    private final Logger logger;

    public AiSkillLoader(AiSkillParser parser, Logger logger) {
        this.parser = java.util.Objects.requireNonNull(parser, "parser");
        this.logger = java.util.Objects.requireNonNull(logger, "logger");
    }

    public AiSkillCatalog load(ClassLoader resources, String manifest, Path customRoot) {
        java.util.Objects.requireNonNull(resources, "resources");
        java.util.Objects.requireNonNull(manifest, "manifest");
        java.util.Objects.requireNonNull(customRoot, "customRoot");

        List<AiSkill> loaded = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        loadBuiltIns(resources, manifest, loaded, ids);
        loadCustom(customRoot, loaded, ids);
        return new AiSkillCatalog(loaded);
    }

    private void loadBuiltIns(
            ClassLoader resources, String manifest, List<AiSkill> loaded, Set<String> ids) {
        String content = readRequiredResource(resources, manifest);
        for (String rawLine : content.replace("\r\n", "\n").split("\n")) {
            String path = rawLine.trim();
            if (path.isEmpty() || path.startsWith("#")) {
                continue;
            }
            if (!RESOURCE_PATH.matcher(path).matches() || path.contains("..") || path.contains("\\")) {
                throw new AiSkillFormatException(manifest, "Unsafe Skill resource path '" + path + "'");
            }
            try {
                AiSkill skill = parser.parse(path, readRequiredResource(resources, path), AiSkillSource.BUILT_IN);
                if (!ids.add(skill.id())) {
                    logger.error("Ignoring duplicate built-in AI Skill id {} from {}", skill.id(), path);
                    continue;
                }
                loaded.add(skill);
            } catch (RuntimeException exception) {
                logger.error("Ignoring invalid built-in AI Skill {}: {}", path, exception.getMessage());
            }
        }
    }

    private void loadCustom(Path configuredRoot, List<AiSkill> loaded, Set<String> ids) {
        Path root = configuredRoot.toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            logger.warn("Unable to create custom AI Skill directory {}: {}", root, exception.getMessage());
            return;
        }

        List<Path> candidates;
        try (Stream<Path> entries = Files.list(root)) {
            candidates = entries
                    .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".md"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException exception) {
            logger.warn("Unable to scan custom AI Skill directory {}: {}", root, exception.getMessage());
            return;
        }

        int totalBytes = 0;
        int processed = 0;
        for (Path candidate : candidates) {
            if (processed >= MAX_CUSTOM_FILES) {
                logger.warn("Ignoring custom AI Skill {} because the {} file limit was reached",
                        candidate.getFileName(), MAX_CUSTOM_FILES);
                continue;
            }
            processed++;
            if (!safeRegularFile(root, candidate)) {
                logger.warn("Ignoring unsafe custom AI Skill path {}", candidate);
                continue;
            }
            try {
                byte[] bytes = Files.readAllBytes(candidate);
                if (totalBytes + bytes.length > MAX_CUSTOM_TOTAL_BYTES) {
                    logger.warn("Ignoring custom AI Skill {} because the 512 KiB total limit was reached",
                            candidate.getFileName());
                    continue;
                }
                totalBytes += bytes.length;
                AiSkill skill = parser.parse(candidate.toString(), decode(candidate.toString(), bytes), AiSkillSource.CUSTOM);
                if (!ids.add(skill.id())) {
                    logger.warn("Ignoring duplicate custom AI Skill id {} from {}", skill.id(), candidate.getFileName());
                    continue;
                }
                loaded.add(skill);
            } catch (IOException | RuntimeException exception) {
                logger.warn("Ignoring invalid custom AI Skill {}: {}", candidate.getFileName(), exception.getMessage());
            }
        }
    }

    private static boolean safeRegularFile(Path root, Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        return normalized.getParent() != null
                && normalized.getParent().equals(root)
                && normalized.startsWith(root)
                && !Files.isSymbolicLink(candidate)
                && Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS);
    }

    private static String readRequiredResource(ClassLoader resources, String path) {
        try (InputStream stream = resources.getResourceAsStream(path)) {
            if (stream == null) {
                throw new AiSkillFormatException(path, "Required Skill resource is missing");
            }
            return decode(path, stream.readAllBytes());
        } catch (IOException exception) {
            throw new AiSkillFormatException(path, "Unable to read Skill resource: " + exception.getMessage());
        }
    }

    private static String decode(String origin, byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new AiSkillFormatException(origin, "Skill file is not valid UTF-8");
        }
    }
}
