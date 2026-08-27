package com.mohistmc.youer.ai.tool;

import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.permissions.DefaultPermissions;

public final class AiToolPermissions {
    private AiToolPermissions() {}
    public static void registerDefaults() {
        ensureOpDefault("youer.ai.tools.use", "Allows use of AI tools");
        ensureOpDefault("youer.ai.tools.command.player", "Allows AI player command execution");
        ensureOpDefault("youer.ai.tools.command.console", "Allows AI console command execution");
        ensureOpDefault("youer.ai.skills.use", "Allows use of AI Skills");
    }
    public static void ensureOpDefault(String node) { ensureOpDefault(node, "AI tool permission"); }
    private static void ensureOpDefault(String node, String description) {
        if (Bukkit.getPluginManager().getPermission(node) == null) {
            DefaultPermissions.registerPermission(node, description, PermissionDefault.OP);
        }
    }
}
