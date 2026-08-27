package com.mohistmc.youer.ai.skill;

public final class AiSkillFormatException extends IllegalArgumentException {

    public AiSkillFormatException(String origin, String message) {
        super(origin + ": " + message);
    }
}
