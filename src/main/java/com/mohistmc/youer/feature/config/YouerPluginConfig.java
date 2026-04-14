package com.mohistmc.youer.feature.config;

import com.mohistmc.youer.util.YamlUtils;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class YouerPluginConfig {

    public static final String CONFIG_FILE = "youer-config";
    public final FileConfiguration yaml;
    public final File config;

    public YouerPluginConfig(File file) {
        config = file;
        yaml = YamlConfiguration.loadConfiguration(config);
        if (!config.exists()) {
            save();
        }
    }

    public YouerPluginConfig(String yml) {
        config = new File(YouerPluginConfig.CONFIG_FILE, yml);
        yaml = YamlConfiguration.loadConfiguration(config);
        if (!config.exists()) {
            save();
        }
    }

    public void save() {
        YamlUtils.save(config, yaml);
    }

    public void put(String key, Object v) {
        yaml.set(key, v);
        save();
    }

    public void remove(String name) {
        yaml.set(name, null);
        save();
    }

    public boolean has(String name) {
        return yaml.get(name) != null;
    }
}
