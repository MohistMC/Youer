package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.api.ServerAPI;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.legacy.FieldRename;
import org.bukkit.craftbukkit.util.ApiVersion;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.EntityType;

public class CraftEntityType {

    public static EntityType minecraftToBukkit(net.minecraft.world.entity.EntityType<?> minecraft) {
        Preconditions.checkArgument(minecraft != null);
        if (ServerAPI.entityTypeMap0.containsKey(minecraft)) {
            return ServerAPI.entityTypeMap0.get(minecraft);
        }
        net.minecraft.core.Registry<net.minecraft.world.entity.EntityType<?>> registry = CraftRegistry.getMinecraftRegistry(Registries.ENTITY_TYPE);
        NamespacedKey key = CraftNamespacedKey.fromMinecraft(registry.getResourceKey(minecraft).orElseThrow().location());
        EntityType bukkit = Registry.ENTITY_TYPE.get(key);

        Preconditions.checkArgument(bukkit != null);

        return bukkit;
    }

    private static final java.util.Map<EntityType, net.minecraft.resources.ResourceKey<net.minecraft.world.entity.EntityType<?>>> KEY_CACHE = java.util.Collections.synchronizedMap(new java.util.EnumMap<>(EntityType.class)); // Paper
    public static net.minecraft.world.entity.EntityType<?> bukkitToMinecraft(EntityType bukkit) {
        Preconditions.checkArgument(bukkit != null);
        return CraftRegistry.getMinecraftRegistry(Registries.ENTITY_TYPE)
                .getOptional(KEY_CACHE.computeIfAbsent(bukkit, type -> net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE, CraftNamespacedKey.toMinecraft(type.getKey())))).orElseThrow();
    }

    public static String bukkitToString(EntityType bukkit) {
        Preconditions.checkArgument(bukkit != null);

        return bukkit.getKey().toString();
    }

    public static EntityType stringToBukkit(String string) {
        Preconditions.checkArgument(string != null);

        // We currently do not have any version-dependent remapping, so we can use current version
        // First convert from when only the names where saved
        string = FieldRename.convertEntityTypeName(ApiVersion.CURRENT, string);
        string = string.toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(string);

        // Now also convert from when keys where saved
        return CraftRegistry.get(Registry.ENTITY_TYPE, key, ApiVersion.CURRENT);
    }
}
