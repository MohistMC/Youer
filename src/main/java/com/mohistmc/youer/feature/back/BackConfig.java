package com.mohistmc.youer.feature.back;

import com.mohistmc.youer.feature.commands.CommandsConfig;
import com.mohistmc.youer.feature.db.BackDatabaseStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author Mgazul by MohistMC
 * <p>
 * Player back-location storage backed by SQLite database.
 * Always uses SQLite (not configurable) — simple structured data, no Zstd needed.
 */
public class BackConfig {

    public static BackConfig INSTANCE;
    private final BackDatabaseStorage storage;

    public BackConfig() {
        this.storage = new BackDatabaseStorage();
    }

    public static void init() {
        INSTANCE = new BackConfig();
        INSTANCE.storage.init();
    }

    public void saveLocation(Player player, Location location, BackType backType) {
        if (!CommandsConfig.INSTANCE.enable("back.enable")) return;
        storage.saveLocation(
                player.getUniqueId().toString(),
                location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getPitch(), location.getYaw(),
                backType.name()
        );
    }

    public Location getLocation(Player player) {
        return storage.getLocation(player.getUniqueId().toString());
    }

    public BackType getBackType(Player player) {
        String type = storage.getBackType(player.getUniqueId().toString());
        return type != null ? BackType.valueOf(type) : null;
    }

    public boolean has(String uuid) {
        return storage.has(uuid);
    }
}
