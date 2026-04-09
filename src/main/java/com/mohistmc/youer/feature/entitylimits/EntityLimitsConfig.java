package com.mohistmc.youer.feature.entitylimits;

import com.mohistmc.youer.api.WorldAPI;
import com.mohistmc.youer.feature.config.YouerPluginConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.bukkit.World;

/**
 * @author Mgazul
 * @date 2025/10/2 21:30
 */
public class EntityLimitsConfig extends YouerPluginConfig {

    public static EntityLimitsConfig INSTANCE;

    public EntityLimitsConfig(File file) {
        super(file);
    }

    public static void init() {
        INSTANCE = new EntityLimitsConfig(new File("youer-config", "entitylimits.yml"));
    }

    public boolean hasWorld(String worldName) {
        return yaml.contains(worldName);
    }

    public List<EntityLimits> getEntityLimits() {
        List<EntityLimits> entityLimits = new ArrayList<>();
        for (String worldName : yaml.getKeys(false)) {
            for (String entityName : yaml.getConfigurationSection(worldName).getKeys(false)) {
                int entityLimit = yaml.getInt(worldName + "." + entityName, -1);
                entityLimits.add(new EntityLimits(worldName, entityName, entityLimit));
            }
        }
        return entityLimits;
    }

    public void remove(String worldName, String entityName) {
        yaml.set(worldName + "." + entityName, null);
        save();
    }

    public void set(String worldName, String entityName, int entityLimit) {
        yaml.set(worldName + "." + entityName, entityLimit);
        save();
    }

    public boolean canSpawn(Entity entity) {
        World world = entity.level().getWorld();
        boolean hasLimit = yaml.contains(world.getName() + "." + entity.getBukkitEntity().getType().name());
        if (!hasLimit) {
            return false;
        }
        Map<EntityType<?>, Integer> collect =
                StreamSupport.stream(WorldAPI.getServerLevel(world).getAllEntities().spliterator(), false)
                        .collect(Collectors.toMap(
                                Entity::getType,
                                entity1 -> 1,
                                Integer::sum
                        ));
        int limit = yaml.getInt(world.getName() + "." + entity.getBukkitEntity().getType().name(), -1);
        int entitySize = collect.getOrDefault(entity.getType(), 0);
        return entitySize >= limit;
    }
}
