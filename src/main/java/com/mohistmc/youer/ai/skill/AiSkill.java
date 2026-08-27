package com.mohistmc.youer.ai.skill;

import com.mohistmc.youer.api.ai.tool.AiToolRisk;
import java.util.List;
import java.util.Objects;

public record AiSkill(
        String id,
        String title,
        String description,
        String edition,
        String minecraftVersion,
        List<String> commands,
        List<String> tools,
        AiSkillExecution execution,
        AiToolRisk risk,
        String documentationSource,
        String body,
        AiSkillSource source,
        String origin) {

    public AiSkill {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        commands = List.copyOf(commands);
        tools = List.copyOf(tools);
        Objects.requireNonNull(execution, "execution");
        Objects.requireNonNull(risk, "risk");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(origin, "origin");
    }
}
