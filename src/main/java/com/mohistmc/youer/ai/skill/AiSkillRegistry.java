package com.mohistmc.youer.ai.skill;

import com.mohistmc.youer.ai.tool.AiToolPermissions;
import com.mohistmc.youer.ai.tool.AiToolRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AiSkillRegistry {

    private static final List<String> GLOBAL_PERMISSIONS = List.of(
            "youer.ai.use", "youer.ai.tools.use", "youer.ai.skills.use");
    private final AiSkillCatalog catalog;

    public AiSkillRegistry(AiSkillCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public AiSkillSnapshot snapshot(AiSkillAccess access, AiToolRegistry.Snapshot tools) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(tools, "tools");
        if (GLOBAL_PERMISSIONS.stream().anyMatch(permission -> !access.hasPermission(permission))) {
            return new AiSkillSnapshot(List.of());
        }
        List<AiSkill> visible = new ArrayList<>();
        for (AiSkill skill : catalog.skills()) {
            if (visible(skill, access, tools)) {
                visible.add(skill);
            }
        }
        return new AiSkillSnapshot(visible);
    }

    public void registerPermissions() {
        AiToolPermissions.ensureOpDefault("youer.ai.skills.use");
        catalog.skills().stream()
                .filter(skill -> skill.source() == AiSkillSource.CUSTOM)
                .map(AiSkillRegistry::customPermission)
                .forEach(AiToolPermissions::ensureOpDefault);
    }

    public static String customPermission(AiSkill skill) {
        if (skill.source() != AiSkillSource.CUSTOM || !skill.id().startsWith("custom.")) {
            throw new IllegalArgumentException("Skill is not a custom Skill: " + skill.id());
        }
        return "youer.ai.skills." + skill.id();
    }

    private static boolean visible(
            AiSkill skill, AiSkillAccess access, AiToolRegistry.Snapshot tools) {
        if (skill.source() == AiSkillSource.CUSTOM && !access.hasPermission(customPermission(skill))) {
            return false;
        }
        if ((skill.execution() == AiSkillExecution.CONSOLE || skill.execution() == AiSkillExecution.MIXED)
                && !tools.has("execute_console_command")) {
            return false;
        }
        if (skill.tools().stream().anyMatch(tool -> !tools.has(tool))) {
            return false;
        }
        for (String command : skill.commands()) {
            if (!access.commandRegistered(command) || !access.canUseCommand(command)) {
                return false;
            }
        }
        return true;
    }
}
