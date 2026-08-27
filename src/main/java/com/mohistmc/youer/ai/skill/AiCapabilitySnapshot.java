package com.mohistmc.youer.ai.skill;

import com.mohistmc.youer.ai.tool.AiToolRegistry;
import java.util.Objects;

public record AiCapabilitySnapshot(
        AiToolRegistry.Snapshot tools,
        AiSkillSnapshot skills,
        String systemContext) {

    public AiCapabilitySnapshot {
        Objects.requireNonNull(tools, "tools");
        Objects.requireNonNull(skills, "skills");
        Objects.requireNonNull(systemContext, "systemContext");
    }
}
