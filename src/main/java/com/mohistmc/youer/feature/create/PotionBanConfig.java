package com.mohistmc.youer.feature.create;

import com.mohistmc.youer.feature.config.YouerPluginConfig;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Potion blacklist config for MixinGenericItemEmptying (Create item drain emptying block).
 * Hot path reads use a static cache refreshed on write, same pattern as BanConfig's globalCache.
 *
 * @author Mgazul
 * @date 2026/8/7
 */
public class PotionBanConfig extends YouerPluginConfig {

    private static final String KEY = "blocked-potions";

    public static PotionBanConfig INSTANCE;

    private static volatile Set<String> cache = new HashSet<>();

    public PotionBanConfig(File file) {
        super(file);
        refreshCache();
    }

    public static void init() {
        INSTANCE = new PotionBanConfig(new File("youer-config", "item-drain-potionban.yml"));
    }

    // Youer start - hot path: O(1) contains lookup
    public static Set<String> getBlockedPotions() {
        return cache;
    }
    // Youer end

    public boolean has(String id) {
        return cache.contains(id);
    }

    public boolean addIfAbsent(String id) {
        if (cache.contains(id)) {
            return false;
        }
        add(id);
        return true;
    }

    public void add(String id) {
        List<String> list = yaml.getStringList(KEY);
        list.add(id);
        yaml.set(KEY, list);
        save();
        refreshCache();
    }

    public void remove(String id) {
        List<String> list = yaml.getStringList(KEY);
        list.remove(id);
        if (list.isEmpty()) {
            yaml.set(KEY, null);
        } else {
            yaml.set(KEY, list);
        }
        save();
        refreshCache();
    }

    private void refreshCache() {
        List<String> list = yaml.getStringList(KEY);
        cache = (list == null || list.isEmpty()) ? new HashSet<>() : new HashSet<>(list);
    }
}
