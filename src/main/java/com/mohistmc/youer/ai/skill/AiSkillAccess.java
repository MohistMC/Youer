package com.mohistmc.youer.ai.skill;

public interface AiSkillAccess {
    boolean hasPermission(String permission);
    boolean commandRegistered(String command);
    boolean canUseCommand(String command);
}
