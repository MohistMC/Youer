package com.mohistmc.youer.neoforge;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.mohistmc.dynamicenum.MohistDynamEnum;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.api.ServerAPI;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.dimension.LevelStem;
import org.bukkit.Art;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.CraftSpawnCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionType;

public class NeoForgeInjectBukkit {

    public static final boolean DEBUG = Boolean.getBoolean("youer.debug");
    public static BiMap<ResourceKey<LevelStem>, World.Environment> environment =
            HashBiMap.create(ImmutableMap.<ResourceKey<LevelStem>, World.Environment>builder()
                    .put(LevelStem.OVERWORLD, World.Environment.NORMAL)
                    .put(LevelStem.NETHER, World.Environment.NETHER)
                    .put(LevelStem.END, World.Environment.THE_END)
                    .build());

    public static BiMap<World.Environment, ResourceKey<LevelStem>> environment0 =
            HashBiMap.create(ImmutableMap.<World.Environment, ResourceKey<LevelStem>>builder()
                    .put(World.Environment.NORMAL, LevelStem.OVERWORLD)
                    .put(World.Environment.NETHER, LevelStem.NETHER)
                    .put(World.Environment.THE_END, LevelStem.END)
                    .build());

    public static Map<Villager.Profession, Identifier> profession = new HashMap<>();
    public static Map<StatType<?>, Statistic> statisticMap = new HashMap<>();
    public static Map<MobCategory, SpawnCategory> spawnCategoryMap = new HashMap<>();
    public static Map<SpawnCategory, MobCategory> CategoryspawnMap = new HashMap<>();


    public static void init() {
        addEnumMaterialInItems();
        addEnumEffectAndPotion();
        addEnumMaterialsInBlocks();
        addEnumEntity();
        addEnumArt();
        //addEnumParticle();
        addStatistic();
        loadSpawnCategory();
        addPose();
        reloadBukkitRegistries();
    }

    private static String getMaterialName(Identifier resourceLocation, boolean isMod) {
        return isMod ?
                MohistDynamEnum.normalizeName(resourceLocation.toString()) :
                MohistDynamEnum.normalizeName(resourceLocation.getPath());
    }

    public static void addEnumMaterialInItems() {
        var registry = BuiltInRegistries.ITEM;
        List<String> materials = new ArrayList<>(Arrays.stream(Material.values())
                .map(Enum::name)
                .toList());
        for (Item item : registry) {
            Identifier resourceLocation = registry.getKey(item);
            boolean isMod = isMods(resourceLocation);
            String materialName = getMaterialName(resourceLocation, isMod);

            if (isMod || !materials.contains(materialName)) {
                int id = Item.getId(item);

                Material material = Material.addMaterial(materialName, id, false, true, resourceLocation);

                if (material != null) {
                    CraftMagicNumbers.ITEM_MATERIAL.put(item, material);
                    CraftMagicNumbers.MATERIAL_ITEM.put(material, item);
                    debug("Save-ITEM: {} - {}", material.name(), material.getKeyOrNull());
                }
            }
        }
        materials.clear();
    }

    public static void addEnumMaterialsInBlocks() {
        var registry = BuiltInRegistries.BLOCK;
        List<String> materials = new ArrayList<>(Arrays.stream(Material.values())
                .map(Enum::name)
                .toList());
        for (Block block : registry) {
            Identifier resourceLocation = registry.getKey(block);
            boolean isMod = isMods(resourceLocation);
            String materialName = getMaterialName(resourceLocation, isMod);

            // 检查是否需要添加材料
            if (isMod || !materials.contains(materialName)) {
                int id = Item.getId(block.asItem());
                Item item = Item.byId(id);

                Material material = Material.addMaterial(materialName, id, true, false, resourceLocation);
                if (material != null) {
                    CraftMagicNumbers.BLOCK_MATERIAL.put(block, material);
                    CraftMagicNumbers.MATERIAL_BLOCK.put(material, block);
                    debug("Save-BLOCK:{} - {}", material.name(), material.getKeyOrNull());
                }
            }
        }
        materials.clear();
    }

    public static void addEnumMaterialsInBlockEntityType() {
        var registry = BuiltInRegistries.BLOCK_ENTITY_TYPE;
        for (BlockEntityType<?> entityType : registry) {
            Identifier resourceLocation = registry.getKey(entityType);
            if (isMods(resourceLocation)) {
                String materialName = MohistDynamEnum.normalizeName(resourceLocation.toString());
                Youer.LOGGER.error("Discover entity blocks:{} - {}", entityType, materialName);
            }
        }
    }

    public static void addEnumEffectAndPotion() {
        var registry = BuiltInRegistries.POTION;
        for (Potion potion : registry) {
            Identifier resourceLocation = registry.getKey(potion);
            if (resourceLocation != null) {
                String name = MohistDynamEnum.normalizeName(resourceLocation.toString());
                if (isMods(resourceLocation)) {
                    try {
                        PotionType.valueOf(name);
                    } catch (Exception e) {
                        PotionType potionType = MohistDynamEnum.addEnum(PotionType.class, name, List.of(String.class), List.of(resourceLocation.toString()));
                        if (potionType != null) {
                            CraftPotionUtil.mods.put(resourceLocation, potionType);
                            debug("Save-PotionType:{} - {}", name, potionType.name());
                        }
                    }
                }
            }
        }
    }

    public static void addEnumParticle() {
        var registry = BuiltInRegistries.PARTICLE_TYPE;
        for (ParticleType<?> particleType : registry) {
            Identifier resourceLocation = registry.getKey(particleType);
            String name = MohistDynamEnum.normalizeName(resourceLocation.toString());
            if (!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                Particle particle = MohistDynamEnum.addEnum(Particle.class, name);
                if (particle != null) {
                    debug("Save-ParticleType:{} - {}", name, particle.name());
                }
            }
        }
    }


    public static void addEnumEnvironment(Registry<LevelStem> registry) {
        int i = World.Environment.values().length;
        for (Entry<ResourceKey<LevelStem>, LevelStem> entry : registry.entrySet()) {
            ResourceKey<LevelStem> key = entry.getKey();
            World.Environment environment1 = environment.get(key);
            if (environment1 == null) {
                String name = MohistDynamEnum.normalizeName(key.identifier().toString());
                int id = i - 1;
                environment1 = MohistDynamEnum.addEnum(World.Environment.class, name, List.of(Integer.TYPE), List.of(id));
                environment.put(key, environment1);
                environment0.put(environment1, key);
                debug("Registered forge DimensionType as environment {}", environment1);
                i++;
            }
        }
    }

    public static void addEnumEntity() {
        var registry = BuiltInRegistries.ENTITY_TYPE;
        List<String> entityTypeNames = Arrays.stream(EntityType.values())
                .map(Enum::name)
                .toList();
        for (net.minecraft.world.entity.EntityType<?> entity : registry) {
            Identifier resourceLocation = registry.getKey(entity);
            if (resourceLocation == null) continue;
            boolean isMod = isMods(resourceLocation);
            String entityName = getMaterialName(resourceLocation, isMod);
            if (isMod) {
                int typeId = entityName.hashCode();
                EntityType bukkitType = MohistDynamEnum.addEnum(EntityType.class, entityName,
                        List.of(String.class, Class.class, Integer.TYPE, Boolean.TYPE),
                        List.of(entityName.toLowerCase(), Entity.class, typeId, false));

                if (bukkitType != null) {
                    bukkitType.hookForgeEntity(resourceLocation, entity);
                }
                debug("Registered forge EntityType as {}", bukkitType);
            } else {
                if (!entityTypeNames.contains(entityName)) {
                    int typeId = entityName.hashCode();
                    EntityType bukkitType = MohistDynamEnum.addEnum(EntityType.class, entityName,
                            List.of(String.class, Class.class, Integer.TYPE, Boolean.TYPE),
                            List.of(entityName.toLowerCase(), Entity.class, typeId, false));

                    if (bukkitType != null) {
                        bukkitType.hookForgeEntity(resourceLocation, entity);
                    }
                    debug("Registered mods minecraft key EntityType as {}", bukkitType);
                } else {
                    ServerAPI.entityTypeMap.put(entity, MohistDynamEnum.normalizeName(resourceLocation.getPath()));
                }
            }
        }
    }

    public static void addStatistic() {
        var registry = BuiltInRegistries.STAT_TYPE;
        for (StatType<?> statType : registry) {
            Identifier resourceLocation = registry.getKey(statType);
            if (isMods(resourceLocation)) {
                String name = MohistDynamEnum.normalizeName(resourceLocation.getPath());
                Statistic statistic = MohistDynamEnum.addEnum(Statistic.class, name);
                statisticMap.put(statType, statistic);
                debug("Registered forge StatType as Statistic(Bukkit) {}", statistic.name());
            }
        }
    }

    private static void loadSpawnCategory() {
        for (MobCategory category : MobCategory.values()) {
            try {
                CraftSpawnCategory.toBukkit(category);
            } catch (Exception e) {
                String name = category.name();
                SpawnCategory spawnCategory = MohistDynamEnum.addEnum(SpawnCategory.class, name);
                spawnCategoryMap.put(category, spawnCategory);
                CategoryspawnMap.put(spawnCategory, category);
                spawnCategory.isMods = true;
                debug("Registered forge MobCategory as SpawnCategory(Bukkit) {}", spawnCategory);
            }
        }
    }

    private static void addPose() {
        for (Pose pose : Pose.values()) {
            if (pose.ordinal() > 14) {
                org.bukkit.entity.Pose bukkit = MohistDynamEnum.addEnum(org.bukkit.entity.Pose.class, pose.name());
                debug("Registered forge Pose as Pose(Bukkit) {}", bukkit);
            }
        }
    }

    public static void addEnumArt() {
        int i = Art.values().length;
        var registry = ServerAPI.getNMSServer().registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT);
        for (var entry : registry) {
            int width = entry.width();
            int height = entry.height();
            Identifier resourceLocation = registry.getKey(entry);
            if (isMods(resourceLocation)) {
                String name = MohistDynamEnum.normalizeName(resourceLocation.toString());
                String lookupName = resourceLocation.getPath().toLowerCase(Locale.ROOT);
                int id = i - 1;
                Art art = MohistDynamEnum.addEnum(Art.class, name, List.of(Integer.TYPE, Integer.TYPE, Integer.TYPE), List.of(id, width, height));
                debug("Registered forge PaintingType as Art {}", art);
                i++;
            }
        }
    }

    public static boolean isMods(Identifier resourceLocation) {
        return resourceLocation != null && !resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT);
    }

    public static void reloadBukkitRegistries() {
        try {
            for (var field : org.bukkit.Registry.class.getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.get(null) instanceof org.bukkit.Registry.SimpleRegistry<?> registry) {
                    registry.reload();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void debug(String message, Object p0) {
        if (DEBUG) Youer.LOGGER.debug(message, p0);
    }

    public static void debug(String message, Object p0, Object p1) {
        if (DEBUG) Youer.LOGGER.debug(message, p0, p1);
    }
}
