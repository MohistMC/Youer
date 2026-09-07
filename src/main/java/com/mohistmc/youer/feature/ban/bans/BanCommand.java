package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;

/**
 * @author Mgazul by MohistMC
 * @date 2026/9/6 21:30:00
 */
public class BanCommand {

    private static final java.util.Set<String> PROTECTED_COMMANDS = java.util.Set.of("bans", "youer:bans");

    public static boolean check(String command) {
        if (!YouerConfig.ban_command_enable) return false;
        if (command == null || command.isEmpty()) return false;

        // Strip leading slash if present
        String cmd = command.startsWith("/") ? command.substring(1) : command;

        // Extract the command name (first word before any space)
        String commandName = cmd.split(" ")[0].toLowerCase(java.util.Locale.ENGLISH);

        // Never block protected commands like /bans
        if (PROTECTED_COMMANDS.contains(commandName)) {
            return false;
        }

        var list = BanConfig.getListByType(BanType.COMMAND);
        if (list.isEmpty()) return false;

        for (String banned : list) {
            String bannedLower = banned.toLowerCase(java.util.Locale.ENGLISH);

            // Direct match: "gamemode" == "gamemode"
            if (commandName.equals(bannedLower)) {
                return true;
            }

            // If the banned entry has no namespace, also match "minecraft:gamemode"
            if (!bannedLower.contains(":") && commandName.equals("minecraft:" + bannedLower)) {
                return true;
            }

            // If the banned entry has namespace "minecraft:", also match bare "gamemode"
            if (bannedLower.startsWith("minecraft:") && commandName.equals(bannedLower.substring(10))) {
                return true;
            }
        }

        return false;
    }
}