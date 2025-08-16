package com.mohistmc.youer.api;

import com.mohistmc.youer.plugins.ban.BanConfig;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.EntityType;

public class EntityAPI {

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
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(nmsEntity.getType());
        return key.toString();
    }

    public static boolean isBan(Entity entity) {
        return BanConfig.ENTITY != null && BanConfig.ENTITY.getEntity().contains(resourceLocation(entity));
    }
}
