package com.mohistmc.youer.ai.skill;

import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public final class BukkitAiSkillAccess implements AiSkillAccess {

    private final Player player;

    public BukkitAiSkillAccess(Player player) {
        this.player = player;
    }

    @Override
    public boolean hasPermission(String permission) {
        return player != null && player.isOnline() && player.hasPermission(permission);
    }

    @Override
    public boolean commandRegistered(String command) {
        return findCommand(command) != null;
    }

    @Override
    public boolean canUseCommand(String command) {
        Command registered = findCommand(command);
        return player != null && player.isOnline()
                && registered != null && registered.testPermissionSilent(player);
    }

    private static Command findCommand(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String normalized = label.charAt(0) == '/' ? label.substring(1) : label;
        return Bukkit.getCommandMap().getCommand(normalized.toLowerCase(Locale.ROOT));
    }
}
