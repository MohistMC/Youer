package com.mohistmc.youer.feature.warps;

import com.mohistmc.youer.feature.db.WarpsDatabaseStorage;
import java.util.Set;
import org.bukkit.Location;

/**
 * @author Mgazul by MohistMC
 * @date 2023/9/12 16:39:15
 *
 * Warp storage backed by database.
 */
public class WarpsConfig {

    public static WarpsConfig INSTANCE;
    private final WarpsDatabaseStorage storage;

    public WarpsConfig() {
        this.storage = new WarpsDatabaseStorage();
    }

    public static void init() {
        INSTANCE = new WarpsConfig();
        INSTANCE.storage.init();
    }

    public Location get(String name) {
        return storage.get(name);
    }

    public Set<String> getAllWarpNames() {
        return storage.getAllNames();
    }

    public void put(String name, Object value) {
        if (value instanceof Location loc) {
            storage.set(name, loc);
        }
    }

    public void remove(String name) {
        storage.remove(name);
    }

    public boolean has(String name) {
        return storage.has(name);
    }
}
