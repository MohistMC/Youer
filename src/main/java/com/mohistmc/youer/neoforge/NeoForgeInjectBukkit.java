package com.mohistmc.youer.neoforge;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.api.ServerAPI;
import com.mohistmc.youer.asm.EnumBatcher;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.dimension.LevelStem;
import org.bukkit.Art;
import org.bukkit.Fluid;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.CraftStatistic;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerRecipeBookSettingsChangeEvent;
import org.bukkit.potion.PotionType;

public class NeoForgeInjectBukkit {

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

    public static Map<Villager.Profession, ResourceLocation> profession = new HashMap<>();
    public static Map<ResourceLocation, org.bukkit.attribute.Attribute> attributemap = new HashMap<>();
    public static Map<net.minecraft.world.level.biome.Biome, Biome> biomeBiomeMap = new HashMap<>();
    public static Map<SpawnCategory, MobCategory> CATEGORYSPAWNMAP = new HashMap<>();
    private static final BiMap<ResourceLocation, Statistic> STATISTICS = HashBiMap.create(CraftStatistic.statistics);
    public static final BiMap<SoundEvent, Sound> MODD_SOUNDS = HashBiMap.create();
    public static final BiMap<PaintingVariant, Art> MODD_ART = HashBiMap.create();
    public static Map<String, TreeType> treeTypeByGrowerName = new HashMap<>();

    public static void init() {
        addEnumMaterialInItems();
        addEnumEffectAndPotion();
        addEnumMaterialsInBlocks();
        addEnumBiome();
        addFluid();
        addEnumEntity();
        addEnumArt();
        // addEnumParticle();
        addStatistic();
        loadSpawnCategory();
        addPose();
        addModSound();
        addModRecipeBookType();
        addEnumAttribute();
        addEnumTreeType();
        reloadBukkitRegistries();
    }

    private static String getMaterialName(ResourceLocation resourceLocation, boolean isMod) {
        return isMod ?
                EnumBatcher.normalizeName(resourceLocation.toString()) :
                EnumBatcher.normalizeName(resourceLocation.getPath());
    }

    public static void addEnumMaterialInItems() {
        var registry = BuiltInRegistries.ITEM;
        preCreateItemMaterials(registry);
        for (Item item : registry) {
            ResourceLocation resourceLocation = registry.getKey(item);
            boolean isMod = isMods(resourceLocation);
            if (!isMod && CraftMagicNumbers.getMaterial(item) != null) {
                continue;
            }
            int id = Item.getId(item);
            int maxStackSize = item.getDefaultInstance().getMaxStackSize();
            Material material = Material.addMaterial(getMaterialName(resourceLocation, isMod), id, maxStackSize, false, true, resourceLocation);

            if (material != null) {
                CraftMagicNumbers.ITEM_MATERIAL.put(item, material);
                CraftMagicNumbers.MATERIAL_ITEM.put(material, item);
                debug("Save-ITEM: {} - {}", material.name(), material.key);
            }
        }
    }

    // grow Material once for the whole registry instead of once per entry
    private static void preCreateItemMaterials(Registry<Item> registry) {
        List<String> names = new ArrayList<>();
        List<List<Object>> args = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Item item : registry) {
            ResourceLocation resourceLocation = registry.getKey(item);
            boolean isMod = isMods(resourceLocation);
            if (!isMod && CraftMagicNumbers.getMaterial(item) != null) {
                continue;
            }
            addPendingMaterial(names, args, seen, getMaterialName(resourceLocation, isMod), Item.getId(item), item.getDefaultInstance().getMaxStackSize(), false, true);
        }
        Material.addMaterials(names, args);
    }

    private static void preCreateBlockMaterials(Registry<Block> registry) {
        List<String> names = new ArrayList<>();
        List<List<Object>> args = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Block block : registry) {
            ResourceLocation resourceLocation = registry.getKey(block);
            boolean isMod = isMods(resourceLocation);
            if (!isMod && CraftMagicNumbers.getMaterial(block) != null) {
                continue;
            }
            int id = Item.getId(block.asItem());
            addPendingMaterial(names, args, seen, getMaterialName(resourceLocation, isMod), id, Item.byId(id).getDefaultInstance().getMaxStackSize(), true, false);
        }
        Material.addMaterials(names, args);
    }

    private static void addPendingMaterial(List<String> names, List<List<Object>> args, Set<String> seen, String name, int id, int stack, boolean isBlock, boolean isItem) {
        if (Material.BY_NAME.containsKey(name) || !seen.add(name)) {
            return;
        }
        names.add(name);
        args.add(List.of(id, stack, isBlock, isItem));
    }

    public static void addEnumMaterialsInBlocks() {
        var registry = BuiltInRegistries.BLOCK;
        preCreateBlockMaterials(registry);
        for (Block block : registry) {
            ResourceLocation resourceLocation = registry.getKey(block);
            boolean isMod = isMods(resourceLocation);
            if (!isMod && CraftMagicNumbers.getMaterial(block) != null) {
                continue;
            }
            int id = Item.getId(block.asItem());
            Item item = Item.byId(id);
            int maxStackSize = item.getDefaultInstance().getMaxStackSize();

            Material material = Material.addMaterial(getMaterialName(resourceLocation, isMod), id, maxStackSize, true, false, resourceLocation);
            if (material != null) {
                CraftMagicNumbers.BLOCK_MATERIAL.put(block, material);
                CraftMagicNumbers.MATERIAL_BLOCK.put(material, block);
                debug("Save-BLOCK:{} - {}", material.name(), material.key);
            }
        }
    }

    public static void addEnumEffectAndPotion() {
        var registry = BuiltInRegistries.POTION;
        Set<String> existingPotionTypes = Arrays.stream(PotionType.values()).map(Enum::name).collect(Collectors.toSet());
        for (Potion potion : registry) {
            ResourceLocation resourceLocation = registry.getKey(potion);
            if (resourceLocation != null) {
                String name = EnumBatcher.normalizeName(resourceLocation.toString());
                if (isMods(resourceLocation) && !existingPotionTypes.contains(name)) {
                    PotionType potionType = EnumBatcher.add(PotionType.class, name, List.of(String.class), List.of(resourceLocation.toString()));
                    if (potionType != null) {
                        CraftPotionUtil.mods.put(resourceLocation, potionType);
                        debug("Save-PotionType:{} - {}", name, potionType.name());
                    }
                }
            }
        }
    }

    public static void addEnumParticle() { // TODO CMILIB
        var registry = BuiltInRegistries.PARTICLE_TYPE;
        for (ParticleType<?> particleType : registry) {
            ResourceLocation resourceLocation = registry.getKey(particleType);
            String name = EnumBatcher.normalizeName(resourceLocation.toString());
            if (!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                Particle particle = EnumBatcher.add(Particle.class, name);
                if (particle != null) {
                    debug("Save-ParticleType:{} - {}", name, particle.name());
                }
            }
        }
    }

    public static void addEnumBiome() {
        Set<String> seen = new HashSet<>();
        var registry = ServerAPI.getNMSServer().registryAccess().registryOrThrow(Registries.BIOME);
        for (net.minecraft.world.level.biome.Biome biome : registry) {
            ResourceLocation resourceLocation = registry.getKey(biome);
            String biomeName = EnumBatcher.normalizeName(resourceLocation.toString());
            if (isMods(resourceLocation) && seen.add(biomeName)) {
                org.bukkit.block.Biome biomeCB = EnumBatcher.add(org.bukkit.block.Biome.class, biomeName);
                biomeCB.key = CraftNamespacedKey.fromMinecraft(resourceLocation);
                biomeBiomeMap.put(biome, biomeCB);
                debug("Save-BIOME:{} - {}", biomeCB.name(), biomeName);
            }
        }
    }

    public static void addEnumEnvironment(Registry<LevelStem> registry) {
        int i = World.Environment.values().length;
        for (Entry<ResourceKey<LevelStem>, LevelStem> entry : registry.entrySet()) {
            ResourceKey<LevelStem> key = entry.getKey();
            World.Environment environment1 = environment.get(key);
            if (environment1 == null) {
                String name = EnumBatcher.normalizeName(key.location().toString());
                int id = i - 1;
                environment1 = EnumBatcher.add(World.Environment.class, name, List.of(Integer.TYPE), List.of(id));
                environment.put(key, environment1);
                environment0.put(environment1, key);
                debug("Registered forge DimensionType as environment {}", environment1);
                i++;
            }
        }
    }

    public static void addEnumEntity() {
        var registry = BuiltInRegistries.ENTITY_TYPE;
        Set<String> entityTypeNames = Arrays.stream(EntityType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        for (net.minecraft.world.entity.EntityType<?> entity : registry) {
            ResourceLocation resourceLocation = registry.getKey(entity);
            if (resourceLocation == null) continue;
            boolean isMod = isMods(resourceLocation);
            String entityName = getMaterialName(resourceLocation, isMod);
            if (isMod) {
                int typeId = entityName.hashCode();
                EntityType bukkitType = EnumBatcher.add(EntityType.class, entityName,
                        List.of(String.class, Class.class, Integer.TYPE, Boolean.TYPE),
                        List.of(entityName.toLowerCase(), Entity.class, typeId, false));

                if (bukkitType != null) {
                    bukkitType.hookForgeEntity(resourceLocation, entity);
                }
                debug("Registered forge EntityType as {}", bukkitType);
            } else {
                if (!entityTypeNames.contains(entityName)) {
                    int typeId = entityName.hashCode();
                    EntityType bukkitType = EnumBatcher.add(EntityType.class, entityName,
                            List.of(String.class, Class.class, Integer.TYPE, Boolean.TYPE),
                            List.of(entityName.toLowerCase(), Entity.class, typeId, false));

                    if (bukkitType != null) {
                        bukkitType.hookForgeEntity(resourceLocation, entity);
                    }
                    debug("Registered mods minecraft key EntityType as {}", bukkitType);
                } else {
                    ServerAPI.entityTypeMap.put(entity, EnumBatcher.normalizeName(resourceLocation.getPath()));
                }
            }
        }
        EntityClassLookup.init();
    }

    public static void addEnumAttribute() {
        var registry = BuiltInRegistries.ATTRIBUTE;
        for (Attribute attribute : registry) {
            ResourceLocation resourceLocation = registry.getKey(attribute);
            if (isMods(resourceLocation)) {
                String name = EnumBatcher.normalizeName(resourceLocation.getPath());
                org.bukkit.attribute.Attribute ab = EnumBatcher.add(org.bukkit.attribute.Attribute.class, name, List.of(String.class), List.of(resourceLocation.toString()));
                if (ab != null) {
                    attributemap.put(resourceLocation, ab);
                    debug("Registered forge Attribute as Attribute(Bukkit) {}", ab.name());
                }
            }
        }
    }

    public static void addFluid() {
        var registry = BuiltInRegistries.FLUID;
        for (net.minecraft.world.level.material.Fluid fluidType : registry) {
            ResourceLocation resourceLocation = registry.getKey(fluidType);
            if (isMods(resourceLocation)) {
                String name = EnumBatcher.normalizeName(resourceLocation.getPath());
                Fluid fluid = EnumBatcher.add(Fluid.class, name);
                debug("Registered forge Fluid as Fluid(Bukkit) {}", fluid.name());
            }
        }
    }

    public static void addStatistic() {
        var registry = BuiltInRegistries.STAT_TYPE;
        for (StatType<?> statType : registry) {
            if (statType == Stats.CUSTOM) continue;
            var resourceLocation = registry.getKey(statType);
            Statistic statistic = STATISTICS.get(resourceLocation);
            if (statistic == null && isMods(resourceLocation)) {
                String name = EnumBatcher.normalizeName(resourceLocation.getPath());
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
                statistic = EnumBatcher.add(Statistic.class, name, List.of(Statistic.Type.class), List.of(type));
                statistic.key = NamespacedKey.fromString(resourceLocation.toString());
                STATISTICS.put(resourceLocation, statistic);
                debug("Registered forge STAT_TYPE as Statistic(Bukkit) {}", name);
            }
        }
        for (ResourceLocation resourceLocation : BuiltInRegistries.CUSTOM_STAT) {
            Statistic statistic = STATISTICS.get(resourceLocation);
            if (statistic == null && isMods(resourceLocation)) {
                String name = EnumBatcher.normalizeName(resourceLocation.getPath());
                statistic = EnumBatcher.add(Statistic.class, name);
                statistic.key = NamespacedKey.fromString(resourceLocation.toString());
                STATISTICS.put(resourceLocation, statistic);
                debug("Registered forge CUSTOM_STAT as Statistic(Bukkit) {}", name);
            }
        }
        CraftStatistic.statistics = STATISTICS;
    }

    private static void loadSpawnCategory() {
        Set<String> existing = Arrays.stream(SpawnCategory.values()).map(Enum::name).collect(Collectors.toSet());
        for (MobCategory category : MobCategory.values()) {
            String name = category.name();
            if (existing.contains(name)) continue;
            SpawnCategory spawnCategory = EnumBatcher.add(SpawnCategory.class, name);
            CATEGORYSPAWNMAP.put(spawnCategory, category);
            spawnCategory.isMods = true;
            debug("Registered forge MobCategory as SpawnCategory(Bukkit) {}", spawnCategory);
        }
    }

    private static void addPose() {
        for (Pose pose : Pose.values()) {
            if (pose.ordinal() > 14) {
                org.bukkit.entity.Pose bukkit = EnumBatcher.add(org.bukkit.entity.Pose.class, pose.name());
                debug("Registered forge Pose as Pose(Bukkit) {}", bukkit);
            }
        }
    }

    public static void addEnumArt() {
        int i = Art.values().length;
        var registry = ServerAPI.getNMSServer().registryAccess().registryOrThrow(Registries.PAINTING_VARIANT);
        for (var entry : registry) {
            int width = entry.width();
            int height = entry.height();
            ResourceLocation resourceLocation = registry.getKey(entry);
            if (resourceLocation != null) {
                boolean isMod = isMods(resourceLocation);
                String name = getMaterialName(resourceLocation, isMod);
                var bukkitKey = CraftNamespacedKey.fromMinecraft(resourceLocation);
                if (isMod || !Art.BY_KEY.containsKey(bukkitKey)) {
                    String lookupName = resourceLocation.getPath().toLowerCase(Locale.ROOT);
                    int id = i - 1;
                    Art art = EnumBatcher.add(Art.class, name, List.of(Integer.TYPE, Integer.TYPE, Integer.TYPE), List.of(id, width, height));
                    Art.BY_NAME.put(lookupName, art);
                    Art.BY_ID.put(id, art);
                    Art.BY_KEY.put(bukkitKey, art);
                    art.key = bukkitKey;
                    MODD_ART.put(entry, art);
                    debug("Registered forge PaintingType as Art {}", art);
                    i++;
                }
            }
        }
    }

    public static void addModSound() {
        var registry = BuiltInRegistries.SOUND_EVENT;
        for (SoundEvent statType : registry) {
            ResourceLocation resourceLocation = registry.getKey(statType);
            if (isMods(resourceLocation)) {
                String name = resourceLocation.getPath().replace(".", "_").toUpperCase(Locale.ROOT);
                Sound sound = EnumBatcher.add(Sound.class, name, List.of(String.class), List.of(resourceLocation.toString()));
                MODD_SOUNDS.put(statType, sound);
                debug("Registered mods SoundEvent as Sound(Bukkit) {}", sound.name());
            }
        }
    }

    private static void addModRecipeBookType() {
        Set<String> knownTypes = Arrays.stream(PlayerRecipeBookSettingsChangeEvent.RecipeBookType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        for (RecipeBookType type : RecipeBookType.values()) {
            var name = type.name();
            if (!knownTypes.contains(name)) {
                var bukkit = EnumBatcher.add(PlayerRecipeBookSettingsChangeEvent.RecipeBookType.class, name, List.of(), List.of());
                debug("Registered {} as recipe book type {}", name, bukkit);
            }
        }
    }

    public static void addEnumTreeType() {
        for (Map.Entry<String, TreeGrower> entry : TreeGrower.getGrowers().entrySet()) {
            String name = entry.getKey();
            if (!name.contains(":")) continue;

            String enumName = EnumBatcher.normalizeName(name);
            TreeType treeType = EnumBatcher.add(TreeType.class, enumName);
            if (treeType != null) {
                treeTypeByGrowerName.put(name, treeType);
                debug("Registered forge TreeGrower {} as TreeType(Bukkit) {}", name, treeType);
            }
        }
    }

    public static boolean isMods(ResourceLocation resourceLocation) {
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
        if (Youer.DEBUG) Youer.LOGGER.debug(message, p0);
    }

    public static void debug(String message, Object p0, Object p1) {
        if (Youer.DEBUG) Youer.LOGGER.debug(message, p0, p1);
    }
}
