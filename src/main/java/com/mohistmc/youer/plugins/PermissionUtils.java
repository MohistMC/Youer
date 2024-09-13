package com.mohistmc.youer.plugins;

import com.mohistmc.youer.MohistConfig;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.util.I18n;
import com.mohistmc.youer.util.StackTraceUtil;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;

public class PermissionUtils {

    private static final Map<CraftHumanEntity, String> cache_console = new HashMap<>();
    private static final Map<CraftHumanEntity, String> cache_player = new HashMap<>();

    public static void debug(CraftHumanEntity player, StackTraceElement[] elements, String permission) {
        if (MohistConfig.permissions_debug_console) {
            if (cache_console.containsKey(player) && cache_console.get(player).equals(permission)) {
                return;
            }
            cache_console.put(player, permission);
            Youer.LOGGER.warn("Permission Debug: {} {} {}", StackTraceUtil.getCallerMethodInfo(elements), permission, player.getName());
        }
        if (MohistConfig.permissions_send_player) {
            if (cache_player.containsKey(player) && cache_player.get(player).equals(permission)) {
                return;
            }
            cache_player.put(player, permission);
            player.sendMessage(ChatColor.RED + I18n.as("permission.debug.player", permission));
        }
    }
}
