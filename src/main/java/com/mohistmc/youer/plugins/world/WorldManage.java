package com.mohistmc.youer.plugins.world;

import com.mohistmc.youer.api.PlayerAPI;
import com.mohistmc.youer.api.ServerAPI;
import com.mohistmc.youer.plugins.world.utils.ConfigByWorlds;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldManage {

    public static void onEnable() {
        ConfigByWorlds.init();
        ConfigByWorlds.loadWorlds();
        ConfigByWorlds.addWorld(ServerAPI.getNMSServer().server.getServer().getProperties().levelName, false);
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
}
