package com.mohistmc.youer.feature.entitylimits;

import com.mohistmc.youer.api.WorldAPI;
import com.mohistmc.youer.feature.db.EntityLimitsDatabaseStorage;
import java.util.List;
import java.util.stream.StreamSupport;
import net.minecraft.world.entity.Entity;
import org.bukkit.World;

/**
 * @author Mgazul
 * @date 2025/10/2 21:30
 *
 * Entity spawn limits backed by database.
 */
public class EntityLimitsConfig {

    public static EntityLimitsConfig INSTANCE;
    private final EntityLimitsDatabaseStorage storage;

    public EntityLimitsConfig() {
        this.storage = new EntityLimitsDatabaseStorage();
    }

    public static void init() {
        INSTANCE = new EntityLimitsConfig();
        INSTANCE.storage.init();
    }

    public boolean hasWorld(String worldName) {
        return storage.hasWorld(worldName);
    }

    public List<EntityLimits> getEntityLimits() {
        return storage.getAll();
    }

    public void remove(String worldName, String entityName) {
        storage.remove(worldName, entityName);
    }

    public void set(String worldName, String entityName, int entityLimit) {
        storage.set(worldName, entityName, entityLimit);
    }

    public boolean canSpawn(Entity entity) {
        World world = entity.level().getWorld();
        int limit = storage.getLimit(world.getName(), entity.getBukkitEntity().getType().name());
        if (limit < 0) return false;

        long entitySize = StreamSupport.stream(WorldAPI.getServerLevel(world).getAllEntities().spliterator(), false)
                .filter(e -> e.getType() == entity.getType())
                .count();
        return entitySize >= limit;
    }
}
