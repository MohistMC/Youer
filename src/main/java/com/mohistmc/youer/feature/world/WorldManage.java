package com.mohistmc.youer.feature.world;

import com.mohistmc.youer.api.PlayerAPI;
import com.mohistmc.youer.feature.world.utils.ConfigByWorlds;
import com.mohistmc.youer.util.I18n;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class WorldManage {

    public static void onEnable() {
        ConfigByWorlds.init();
        ConfigByWorlds.loadWorlds();
        ConfigByWorlds.addWorld(Bukkit.getUnsafe().getMainLevelName(), false);
        if (Bukkit.getAllowNether()) {
            ConfigByWorlds.addWorld("DIM1", false);
        }
        if (Bukkit.getAllowEnd()) {
            ConfigByWorlds.addWorld("DIM-1", false);
        }
    }

    public static void deleteDir(File path) {
        if (path.exists()) {
            File[] allContents = path.listFiles();
            if (allContents != null) {
                File[] array;
                for (int length = (array = allContents).length, i = 0; i < length; ++i) {
                    File file = array[i];
                    deleteDir(file);
                }
            }
            path.delete();
        }
    }

    public static void changeGameMode(World world, GameMode gameMode) {
        for (Player player : world.getPlayers()) {
            if (!player.isOp()) {
                player.setGameMode(gameMode);
            }
        }
        ConfigByWorlds.setGameMode(world, gameMode.name());
    }

    public static void changeGameMode(ServerPlayer serverPlayer, World world) {
        if (PlayerAPI.isOp(serverPlayer)) return;
        Player player = serverPlayer.getBukkitEntity();
        GameMode gameMode = ConfigByWorlds.getGameMode(world);
        player.setGameMode(Objects.requireNonNullElseGet(gameMode, Bukkit::getDefaultGameMode));
    }

    public static void hookTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (player.isOp()) return;
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;
        if (ConfigByWorlds.isMaintenance(to.getWorld().getName())) {
            event.setCancelled(true);
            player.sendMessage(I18n.as("worldcommands.maintenance.deny"));
        }
    }

    public static void hookChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;
        World to = player.getWorld();
        if (ConfigByWorlds.isMaintenance(to.getName())) {
            player.sendMessage(I18n.as("worldcommands.maintenance.deny"));
            ConfigByWorlds.getSpawn(Bukkit.getUnsafe().getMainLevelName(), player);
        }
    }
}
