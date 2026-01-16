package com.mohistmc.youer.feature.warps;

import com.mohistmc.youer.feature.config.YouerPluginConfig;
import java.io.File;
import java.util.Set;
import org.bukkit.Location;

/**
 * @author Mgazul by MohistMC
 * @date 2023/9/12 16:39:15
 */
public class WarpsConfig extends YouerPluginConfig {

    public static WarpsConfig INSTANCE;

    public WarpsConfig(File file) {
        super(file);
    }

    public static void init() {
        INSTANCE = new WarpsConfig(new File("youer-config", "warps.yml"));
    }

    public Location get(String name) {
        return yaml.getLocation(name);
    }

    public Set<String> getAllWarpNames() {
        return yaml.getKeys(false);
    }
}
