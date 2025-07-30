package com.mohistmc.youer.plugins.back;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.plugins.config.YouerPluginConfig;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class BackConfig extends YouerPluginConfig {

    public static BackConfig INSTANCE;

    public BackConfig(File file) {
        super(file);
    }

    public static void init() {
        INSTANCE = new BackConfig(new File("youer-config", "back.yml"));
    }

    public void saveLocation(Player player, Location location, BackType backType) {
        if (!YouerConfig.back_enable) return;
        yaml.set(player.getUniqueId() + ".location.world", location.getWorld().getName());
        yaml.set(player.getUniqueId() + ".location.x", location.getX());
        yaml.set(player.getUniqueId() + ".location.y", location.getY());
        yaml.set(player.getUniqueId() + ".location.z", location.getZ());
        yaml.set(player.getUniqueId() + ".location.pitch", location.getPitch());
        yaml.set(player.getUniqueId() + ".location.yaw", location.getYaw());
        yaml.set(player.getUniqueId() + ".type", backType.name());
        save();
    }

    public Location getLocation(Player player) {
        World world = Bukkit.getWorld(yaml.getString(player.getUniqueId() + ".location.world"));
        if (world == null) return null;
        double x = yaml.getInt(player.getUniqueId() + ".location.x");
        double y = yaml.getInt(player.getUniqueId() + ".location.y");
        double z = yaml.getInt(player.getUniqueId() + ".location.z");
        float pitch = (float) yaml.getInt(player.getUniqueId() + ".location.pitch");
        float yaw = (float) yaml.getInt(player.getUniqueId() + ".location.yaw");
        return new Location(world, x, y, z, yaw, pitch);
    }

    public BackType getBackType(Player player) {
        return BackType.valueOf(yaml.getString(player.getUniqueId() + ".type"));
    }
}
