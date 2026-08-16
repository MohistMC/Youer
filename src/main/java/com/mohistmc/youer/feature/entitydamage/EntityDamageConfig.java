package com.mohistmc.youer.feature.entitydamage;

import com.mohistmc.youer.feature.config.YouerPluginConfig;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Independent per-entity final damage weakening configuration, stored in
 * {@code youer-config/entitydamage.yml} under the {@code damage} section.
 *
 * <p>The value is the percentage of the entity's final dealt damage that is
 * removed (e.g. 30 means a zombie's final damage is reduced by 30%). An entity
 * not present in the map deals full damage. The map is cached so the hot
 * listener path never touches YAML.</p>
 */
public class EntityDamageConfig extends YouerPluginConfig {

    public static EntityDamageConfig INSTANCE;

    private static volatile Map<String, Integer> weaken = Map.of();

    public EntityDamageConfig(File file) {
        super(file);
    }

    public static void init() {
        INSTANCE = new EntityDamageConfig(new File("youer-config", "entitydamage.yml"));
        reload();
    }

    public static void reload() {
        Map<String, Integer> map = new HashMap<>();
        ConfigurationSection section = INSTANCE.yaml.getConfigurationSection("damage");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int percent = section.getInt(key, -1);
                if (percent > 0 && percent <= 100) {
                    map.put(key, percent);
                }
            }
        }
        weaken = map;
    }

    /**
     * @return the configured weaken percentage for the entity, or {@code null} if not weakened.
     */
    public static Integer getWeakenPercent(String entityName) {
        return weaken.get(entityName);
    }

    public static Map<String, Integer> getWeaken() {
        return weaken;
    }

    public static void setWeaken(String entityName, int percent) {
        INSTANCE.yaml.set("damage." + entityName, percent);
        INSTANCE.save();
        reload();
    }

    public static void removeWeaken(String entityName) {
        INSTANCE.yaml.set("damage." + entityName, null);
        INSTANCE.save();
        reload();
    }
}
