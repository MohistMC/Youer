package com.mohistmc.youer.ai.skill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AiSkillCatalog {

    private final List<AiSkill> skills;
    private final Map<String, AiSkill> byId;

    public AiSkillCatalog(List<AiSkill> skills) {
        this.skills = skills.stream().sorted(java.util.Comparator.comparing(AiSkill::id)).toList();
        Map<String, AiSkill> indexed = new LinkedHashMap<>();
        for (AiSkill skill : this.skills) {
            if (indexed.putIfAbsent(skill.id(), skill) != null) {
                throw new IllegalArgumentException("Duplicate Skill id: " + skill.id());
            }
        }
        this.byId = Map.copyOf(indexed);
    }

    public List<AiSkill> skills() {
        return skills;
    }

    public AiSkill find(String id) {
        return byId.get(id);
    }
}
