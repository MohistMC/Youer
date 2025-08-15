package com.mohistmc.youer.plugins;

import com.mohistmc.youer.YouerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/24 2:54:00
 */
public class KeepInventory {

    public static boolean inventory(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer nmsPlayer) {
            return inventory(nmsPlayer);
        }
        return false;
    }

    public static boolean inventory(ServerPlayer player) {
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return true;
        }
        Player bukkit_player = player.getBukkitEntity();
        if (hasPermission(bukkit_player, Type.INVENTORY)) {
            return true;
        }
        String world = bukkit_player.getWorld().getName();
        boolean i = YouerConfig.keepinventory_global ? YouerConfig.keepinventory_inventory : YouerConfig.yml.getBoolean("keepinventory." + world + ".inventory");
        player.getBukkitEntity().getWorld().setGameRule(GameRule.KEEP_INVENTORY, i);
        return i;
    }

    public static boolean exp(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer nmsPlayer) {
            return exp(nmsPlayer);
        }
        return false;
    }


    public static boolean exp(ServerPlayer player) {
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return true;
        }
        Player bukkit_player = player.getBukkitEntity();
        if (hasPermission(bukkit_player, Type.EXP)) {
            return true;
        }
        String world = bukkit_player.getWorld().getName();
        boolean i = YouerConfig.keepinventory_global ? YouerConfig.keepinventory_exp : YouerConfig.yml.getBoolean("keepinventory." + world + ".exp");
        player.keepLevel = i;
        return i;
    }

    private static boolean hasPermission(Player bukkit_player, Type type) {
        if (!YouerConfig.keepinventory_permission_enable) return false;
        return bukkit_player.hasPermission(type.permission);
    }

    enum Type {
        INVENTORY("keepinventory.permission"),
        EXP("keepinventory.permission");

        final String permission;

        Type(String s) {
            this.permission = s;
        }
    }
}
