package com.mohistmc.youer.api;

import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import java.util.IdentityHashMap;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.EntityType;

public class EntityAPI {

    private static final IdentityHashMap<net.minecraft.world.entity.EntityType<?>, String> KEY_CACHE = new IdentityHashMap<>();

    public static EntityType entityType(String entityName) {
        EntityType type = EntityType.fromName(entityName);
        return Objects.requireNonNullElse(type, EntityType.UNKNOWN);
    }

    public static EntityType entityType(String entityName, EntityType defType) {
        EntityType type = EntityType.fromName(entityName);
        if (type != null) {
            return type;
        } else {
            return defType;
        }
    }

    public static net.minecraft.world.entity.EntityType<?> getType(String resourceLocation) {
        return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(resourceLocation));
    }

    public static String resourceLocation(Entity nmsEntity) {
        return KEY_CACHE.computeIfAbsent(nmsEntity.getType(), t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).toString());
    }

    public static boolean isBan(Entity entity) {
        if (entity == null || BanConfig.ENTITY == null) return false;
        var list = BanConfig.getSetByType(BanType.ENTITY);
        if (list.isEmpty()) return false;
        String entityKey = resourceLocation(entity);
        for (String banned : list) {
            if (banned.endsWith(":*")) {
                if (entityKey.startsWith(banned.substring(0, banned.length() - 1))) {
                    return true;
                }
            } else if (banned.equals(entityKey)) {
                return true;
            }
        }
        return false;
    }
}
