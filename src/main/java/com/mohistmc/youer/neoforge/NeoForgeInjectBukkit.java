package com.mohistmc.youer.neoforge;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.api.ColorAPI;
import com.mohistmc.youer.api.ServerAPI;
import com.mohistmc.youer.util.DynamicEnumUtil;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.LevelStem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftStatistic;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerRecipeBookSettingsChangeEvent;
import org.bukkit.potion.PotionType;

public class NeoForgeInjectBukkit {

    private static final BiMap<Identifier, Statistic> STATISTICS = HashBiMap.create(CraftStatistic.statistics);
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
    public static Map<SpawnCategory, MobCategory> CATEGORYSPAWNMAP = new HashMap<>();


    public static void init() {
        addEnumMaterialInItems();
        addEnumEffectAndPotion();
        addEnumMaterialsInBlocks();
        addEnumEntity();
        addEnumMenuType();
        addStatistic();
        loadSpawnCategory();
        addPose();
        addModRecipeBookType();
        reloadBukkitRegistries();
    }

    private static String getMaterialName(Identifier resourceLocation, boolean isMod) {
        return isMod ?
            DynamicEnumUtil.normalizeName(resourceLocation.toString()) :
            DynamicEnumUtil.normalizeName(resourceLocation.getPath());
    }

    public static void addEnumMaterialInItems() {
        var registry = BuiltInRegistries.ITEM;
        List<String> materials = new ArrayList<>(Arrays.stream(Material.values())
            .map(Enum::name)
            .toList());
        int count = 0;
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
                    count++;
                }
            }
        }
        materials.clear();
        if (count > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod Item(s) §7as Bukkit §bMaterial"), count);
        }
    }

    public static void addEnumMaterialsInBlocks() {
        var registry = BuiltInRegistries.BLOCK;
        List<String> materials = new ArrayList<>(Arrays.stream(Material.values())
            .map(Enum::name)
            .toList());
        int count = 0;
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
                    count++;
                }
            }
        }
        materials.clear();
        if (count > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod Block(s) §7as Bukkit §bMaterial"), count);
        }
    }

    public static void addEnumEffectAndPotion() {
        var registry = BuiltInRegistries.POTION;
        int count = 0;
        for (Potion potion : registry) {
            Identifier resourceLocation = registry.getKey(potion);
            if (resourceLocation != null) {
                String name = DynamicEnumUtil.normalizeName(resourceLocation.toString());
                if (isMods(resourceLocation)) {
                    try {
                        PotionType.valueOf(name);
                    } catch (Exception e) {
                        PotionType potionType = DynamicEnumUtil.addEnum(PotionType.class, name, List.of(NamespacedKey.class), List.of(CraftNamespacedKey.fromMinecraft(resourceLocation)));
                        if (potionType != null) {
                            count++;
                        }
                    }
                }
            }
        }
        if (count > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod Potion(s) §7as Bukkit §bPotionType"), count);
        }
    }

    public static void addEnumParticle() {
        var registry = BuiltInRegistries.PARTICLE_TYPE;
        int count = 0;
        for (ParticleType<?> particleType : registry) {
            Identifier resourceLocation = registry.getKey(particleType);
            String name = DynamicEnumUtil.normalizeName(resourceLocation.toString());
            if (!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                Particle particle = DynamicEnumUtil.addEnum(Particle.class, name);
                if (particle != null) {
                    count++;
                }
            }
        }
        if (count > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod ParticleType(s) §7as Bukkit §bParticle"), count);
        }
    }


    public static void addEnumEnvironment(Registry<LevelStem> registry) {
        int i = World.Environment.values().length;
        int count = 0;
        for (Entry<ResourceKey<LevelStem>, LevelStem> entry : registry.entrySet()) {
            ResourceKey<LevelStem> key = entry.getKey();
            World.Environment environment1 = environment.get(key);
            if (environment1 == null) {
                String name = DynamicEnumUtil.normalizeName(key.identifier().toString());
                int id = i - 1;
                environment1 = DynamicEnumUtil.addEnum(World.Environment.class, name, List.of(Integer.TYPE), List.of(id));
                environment.put(key, environment1);
                environment0.put(environment1, key);
                count++;
                i++;
            }
        }
        if (count > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod DimensionType(s) §7as Bukkit §bEnvironment"), count);
        }
    }

    public static void addEnumEntity() {
        var registry = BuiltInRegistries.ENTITY_TYPE;
        List<String> entityTypeNames = Arrays.stream(EntityType.values())
            .map(Enum::name)
            .toList();
        int modCount = 0;
        int vanillaKeyCount = 0;
        for (net.minecraft.world.entity.EntityType<?> entity : registry) {
            Identifier resourceLocation = registry.getKey(entity);
            if (resourceLocation == null) continue;
            boolean isMod = isMods(resourceLocation);
            String entityName = getMaterialName(resourceLocation, isMod);
            if (isMod) {
                int typeId = entityName.hashCode();
                EntityType bukkitType = DynamicEnumUtil.addEnum(EntityType.class, entityName,
                    List.of(String.class, Class.class, Integer.TYPE, Boolean.TYPE),
                    List.of(entityName.toLowerCase(), Entity.class, typeId, false));

                if (bukkitType != null) {
                    bukkitType.hookForgeEntity(resourceLocation, entity);
                    modCount++;
                }
            } else {
                if (!entityTypeNames.contains(entityName)) {
                    int typeId = entityName.hashCode();
                    EntityType bukkitType = DynamicEnumUtil.addEnum(EntityType.class, entityName,
                        List.of(String.class, Class.class, Integer.TYPE, Boolean.TYPE),
                        List.of(entityName.toLowerCase(), Entity.class, typeId, false));

                    if (bukkitType != null) {
                        bukkitType.hookForgeEntity(resourceLocation, entity);
                        vanillaKeyCount++;
                    }
                } else {
                    ServerAPI.entityTypeMap.put(entity, DynamicEnumUtil.normalizeName(resourceLocation.getPath()));
                }
            }
        }
        if (modCount > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod EntityType(s) §7as Bukkit §bEntityType"), modCount);
        }
        if (vanillaKeyCount > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6minecraft key EntityType(s) §7as Bukkit §bEntityType"), vanillaKeyCount);
        }
    }

    public static void addEnumMenuType() {
        int count = 0;
        for (net.minecraft.world.inventory.MenuType<?> menuType : BuiltInRegistries.MENU) {
            Identifier resourceLocation = BuiltInRegistries.MENU.getKey(menuType);
            if (resourceLocation == null) {
                continue;
            }
            String namespace = resourceLocation.getNamespace();
            if (namespace.equals(NamespacedKey.MINECRAFT)
                    || namespace.equals("neoforge")
                    || namespace.equals("youer")) {
                continue;
            }
            if (com.mohistmc.youer.api.MenuAPI.ensureInventoryType(resourceLocation, menuType) != null) {
                count++;
            }
        }
        if (count > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod MenuType(s) §7as Bukkit §bInventoryType"), count);
        }
    }

    public static void addStatistic() {
        var registry = BuiltInRegistries.STAT_TYPE;
        int statTypeCount = 0;
        for (StatType<?> statType : registry) {
            if (statType == Stats.CUSTOM) continue;
            var resourceLocation = registry.getKey(statType);
            Statistic statistic = STATISTICS.get(resourceLocation);
            if (statistic == null && isMods(resourceLocation)) {
                String name = DynamicEnumUtil.normalizeName(resourceLocation.getPath());
                Statistic.Type type;
                if (statType.getRegistry() == BuiltInRegistries.ENTITY_TYPE) {
                    type = Statistic.Type.ENTITY;
                } else if (statType.getRegistry() == BuiltInRegistries.BLOCK) {
                    type = Statistic.Type.BLOCK;
                } else if (statType.getRegistry() == BuiltInRegistries.ITEM) {
                    type = Statistic.Type.ITEM;
                } else {
                    type = Statistic.Type.UNTYPED;
                }
                statistic = DynamicEnumUtil.addEnum(Statistic.class, name, List.of(Statistic.Type.class), List.of(type));
                statistic.key = NamespacedKey.fromString(resourceLocation.toString());
                STATISTICS.put(resourceLocation, statistic);
                statTypeCount++;
            }
        }
        int customStatCount = 0;
        for (Identifier resourceLocation : BuiltInRegistries.CUSTOM_STAT) {
            Statistic statistic = STATISTICS.get(resourceLocation);
            if (statistic == null && isMods(resourceLocation)) {
                String name = DynamicEnumUtil.normalizeName(resourceLocation.getPath());
                statistic = DynamicEnumUtil.addEnum(Statistic.class, name);
                statistic.key = NamespacedKey.fromString(resourceLocation.toString());
                STATISTICS.put(resourceLocation, statistic);
                customStatCount++;
            }
        }
        CraftStatistic.statistics = STATISTICS;
        if (statTypeCount > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod StatType(s) §7as Bukkit §bStatistic"), statTypeCount);
        }
        if (customStatCount > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod CustomStat(s) §7as Bukkit §bStatistic"), customStatCount);
        }
    }

    private static void loadSpawnCategory() {
        Set<String> existing = Arrays.stream(SpawnCategory.values()).map(Enum::name).collect(Collectors.toSet());
        int count = 0;
        for (MobCategory category : MobCategory.values()) {
            String name = category.name();
            if (existing.contains(name)) continue;
            SpawnCategory spawnCategory = DynamicEnumUtil.addEnum(SpawnCategory.class, name);
            CATEGORYSPAWNMAP.put(spawnCategory, category);
            spawnCategory.isMods = true;
            count++;
        }
        if (count > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod MobCategory(s) §7as Bukkit §bSpawnCategory"), count);
        }
    }

    private static void addPose() {
        int count = 0;
        for (Pose pose : Pose.values()) {
            if (pose.ordinal() > 14) {
                DynamicEnumUtil.addEnum(org.bukkit.entity.Pose.class, pose.name());
                count++;
            }
        }
        if (count > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod Pose(s) §7as Bukkit §bPose"), count);
        }
    }

    private static void addModRecipeBookType() {
        Set<String> knownTypes = Arrays.stream(PlayerRecipeBookSettingsChangeEvent.RecipeBookType.values())
            .map(Enum::name)
            .collect(Collectors.toSet());
        int count = 0;
        for (RecipeBookType type : RecipeBookType.values()) {
            String name = type.name();
            if (!knownTypes.contains(name)) {
                DynamicEnumUtil.addEnum(PlayerRecipeBookSettingsChangeEvent.RecipeBookType.class, name);
                count++;
            }
        }
        if (count > 0) {
            Youer.LOGGER.info(ColorAPI.ansi("§aRegistered {} §6mod RecipeBookType(s) §7as Bukkit §bRecipeBookType"), count);
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
}
