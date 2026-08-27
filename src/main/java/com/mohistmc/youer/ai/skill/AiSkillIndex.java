package com.mohistmc.youer.ai.skill;

import java.util.Locale;

public final class AiSkillIndex {

    public static final int DEFAULT_MAX_SKILLS = 128;
    public static final int DEFAULT_MAX_CHARACTERS = 24_000;
    private static final String HEADER = """
            Available permission-filtered Skills follow. Call load_skill with an exact id before use; actions still require visible tools.
            """;

    private final int maxSkills;
    private final int maxCharacters;

    public AiSkillIndex() {
        this(DEFAULT_MAX_SKILLS, DEFAULT_MAX_CHARACTERS);
    }

    public AiSkillIndex(int maxSkills, int maxCharacters) {
        if (maxSkills < 1 || maxCharacters < HEADER.length() + 32) {
            throw new IllegalArgumentException("Skill index limits are too small");
        }
        this.maxSkills = maxSkills;
        this.maxCharacters = maxCharacters;
    }

    public String format(AiSkillSnapshot snapshot) {
        if (snapshot.skills().isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(HEADER);
        int included = 0;
        for (AiSkill skill : snapshot.skills()) {
            if (included >= maxSkills) {
                break;
            }
            String line = "- %s | %s | execution=%s | risk=%s%n".formatted(
                    skill.id(), compact(skill.description()),
                    skill.execution().name().toLowerCase(Locale.ROOT), skill.risk().name());
            if (result.length() + line.length() > maxCharacters) {
                break;
            }
            result.append(line);
            included++;
        }
        int omitted = snapshot.skills().size() - included;
        if (omitted > 0) {
            String note = "- ... %d additional Skills omitted by index bounds%n".formatted(omitted);
            if (result.length() + note.length() <= maxCharacters) {
                result.append(note);
            }
        }
        return result.toString();
    }

    private static String compact(String value) {
        return value.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }
}
