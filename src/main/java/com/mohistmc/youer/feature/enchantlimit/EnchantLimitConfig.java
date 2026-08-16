package com.mohistmc.youer.feature.enchantlimit;

import com.mohistmc.youer.feature.config.YouerPluginConfig;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Independent per-enchantment max level configuration, stored in
 * {@code youer-config/enchantlimit.yml} under the {@code limits} section.
 *
 * <p>An enchantment not present in the map is unlimited. The map is cached so
 * that the hot {@link EnchantLevelLimiter#clamp} path never touches YAML.</p>
 */
public class EnchantLimitConfig extends YouerPluginConfig {

    public static EnchantLimitConfig INSTANCE;

    private static volatile Map<String, Integer> limits = Map.of();

    public EnchantLimitConfig(File file) {
        super(file);
    }

    public static void init() {
        INSTANCE = new EnchantLimitConfig(new File("youer-config", "enchantlimit.yml"));
        reload();
    }

    public static void reload() {
        Map<String, Integer> map = new HashMap<>();
        ConfigurationSection section = INSTANCE.yaml.getConfigurationSection("limits");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int level = section.getInt(key, -1);
                if (level > 0) {
                    map.put(key, level);
                }
            }
        }
        limits = map;
    }

    /**
     * @return the configured max level for the enchantment, or {@code null} if unlimited.
     */
    public static Integer getMaxLevel(String enchantmentName) {
        return limits.get(enchantmentName);
    }

    public static Map<String, Integer> getLimits() {
        return limits;
    }

    public static void setLimit(String enchantmentName, int level) {
        INSTANCE.yaml.set("limits." + enchantmentName, level);
        INSTANCE.save();
        reload();
    }

    public static void removeLimit(String enchantmentName) {
        INSTANCE.yaml.set("limits." + enchantmentName, null);
        INSTANCE.save();
        reload();
    }
}
