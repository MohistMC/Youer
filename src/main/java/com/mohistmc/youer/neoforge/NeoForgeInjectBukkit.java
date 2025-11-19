package com.mohistmc.youer.neoforge;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.mohistmc.dynamicenum.MohistDynamEnum;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.api.ServerAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.dimension.LevelStem;
import org.bukkit.Art;
import org.bukkit.Fluid;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.craftbukkit.util.CraftSpawnCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.entity.Villager;
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
    public static Map<org.bukkit.attribute.Attribute, ResourceLocation> attributemap = new HashMap<>();
    public static Map<StatType<?>, Statistic> statisticMap = new HashMap<>();
    public static Map<net.minecraft.world.level.biome.Biome, Biome> biomeBiomeMap = new HashMap<>();
    public static Map<MobCategory, SpawnCategory> spawnCategoryMap = new HashMap<>();
    public static Map<SpawnCategory, MobCategory> CategoryspawnMap = new HashMap<>();


    public static void init() {
        addEnumMaterialInItems();
        addEnumEffectAndPotion();
        addEnumMaterialsInBlocks();
        // addEnumMaterialsInBlockEntityType();
        addEnumBiome();
        addFluid();
        addEnumEntity();
        addEnumArt();
        //addEnumParticle();
        addStatistic();
        loadSpawnCategory();
        addPose();
        addModSound();
    }

    private static String getMaterialName(ResourceLocation resourceLocation, boolean isMod) {
        return isMod ?
                MohistDynamEnum.normalizeName(resourceLocation.toString()) :
                MohistDynamEnum.normalizeName(resourceLocation.getPath());
    }

    public static void addEnumMaterialInItems() {
        var registry = BuiltInRegistries.ITEM;
        for (Item item : registry) {
            ResourceLocation resourceLocation = registry.getKey(item);
            boolean isMod = isMods(resourceLocation);
            String materialName = isMod ?
                    MohistDynamEnum.normalizeName(resourceLocation.toString()) :
                    MohistDynamEnum.normalizeName(resourceLocation.getPath());

            Material existingMat = Material.getMaterial(materialName);
            if (isMod || existingMat == null || existingMat.isModBlock) {
                int id = Item.getId(item);
                int maxStackSize = item.getMaxStackSize(new ItemStack(item));

                Material material = Material.addMaterial(materialName, id, maxStackSize, false, true, resourceLocation);

                if (material != null) {
                    CraftMagicNumbers.ITEM_MATERIAL.put(item, material);
                    CraftMagicNumbers.MATERIAL_ITEM.put(material, item);
                    Youer.LOGGER.debug("Save-ITEM: {} - {}", material.name(), material.key);
                }
            }
        }
    }

    public static void addEnumMaterialsInBlocks() {
        var registry = BuiltInRegistries.BLOCK;
        for (Block block : registry) {
            ResourceLocation resourceLocation = registry.getKey(block);
            boolean isMod = isMods(resourceLocation);
            String materialName = isMod ?
                    MohistDynamEnum.normalizeName(resourceLocation.toString()) :
                    MohistDynamEnum.normalizeName(resourceLocation.getPath());

            // 检查是否需要添加材料
            Material existingMat = Material.getMaterial(materialName);
            if (isMod || existingMat == null || existingMat.isModItem) {
                int id = Item.getId(block.asItem());
                Item item = Item.byId(id);
                int maxStackSize = item.getMaxStackSize(new ItemStack(item));

                Material material = Material.addMaterial(materialName, id, maxStackSize, true, false, resourceLocation);
                if (material != null) {
                    CraftMagicNumbers.BLOCK_MATERIAL.put(block, material);
                    CraftMagicNumbers.MATERIAL_BLOCK.put(material, block);
                    Youer.LOGGER.debug("Save-BLOCK:{} - {}", material.name(), material.key);
                }
            }
        }
    }

    public static void addEnumMaterialsInBlockEntityType() {
        var registry = BuiltInRegistries.BLOCK_ENTITY_TYPE;
        for (BlockEntityType<?> entityType : registry) {
            ResourceLocation resourceLocation = registry.getKey(entityType);
            if (isMods(resourceLocation)) {
                String materialName = MohistDynamEnum.normalizeName(resourceLocation.toString());
                Youer.LOGGER.error("Discover entity blocks:{} - {}", entityType, materialName);
            }
        }
    }

    public static void addEnumEffectAndPotion() {
        var registry = BuiltInRegistries.POTION;
        for (Potion potion : registry) {
            ResourceLocation resourceLocation = registry.getKey(potion);
            if (resourceLocation != null) {
                String name = MohistDynamEnum.normalizeName(resourceLocation.toString());
                if (isMods(resourceLocation)) {
                    try {
                        PotionType.valueOf(name);
                    } catch (Exception e) {
                        PotionType potionType = MohistDynamEnum.addEnum(PotionType.class, name, List.of(String.class), List.of(resourceLocation.toString()));
                        if (potionType != null) {
                            CraftPotionUtil.mods.put(resourceLocation, potionType);
                            Youer.LOGGER.debug("Save-PotionType:{} - {}", name, potionType.name());
                        }
                    }
                }
            }
        }
    }

    public static void addEnumParticle() {
        var registry = BuiltInRegistries.PARTICLE_TYPE;
        for (ParticleType<?> particleType : registry) {
            ResourceLocation resourceLocation = registry.getKey(particleType);
            String name = MohistDynamEnum.normalizeName(resourceLocation.toString());
            if (!resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                Particle particle = MohistDynamEnum.addEnum(Particle.class, name);
                if (particle != null) {
                    Youer.LOGGER.debug("Save-ParticleType:{} - {}", name, particle.name());
                }
            }
        }
    }

    public static void addEnumBiome() {
        List<String> map = new ArrayList<>();
        var registry = ServerAPI.getNMSServer().registryAccess().registryOrThrow(Registries.BIOME);
        for (net.minecraft.world.level.biome.Biome biome : registry) {
            ResourceLocation resourceLocation = registry.getKey(biome);
            String biomeName = MohistDynamEnum.normalizeName(resourceLocation.toString());
            if (isMods(resourceLocation) && !map.contains(biomeName)) {
                map.add(biomeName);
                org.bukkit.block.Biome biomeCB = MohistDynamEnum.addEnum(org.bukkit.block.Biome.class, biomeName);
                biomeCB.key = CraftNamespacedKey.fromMinecraft(resourceLocation);
                biomeBiomeMap.put(biome, biomeCB);
                Youer.LOGGER.debug("Save-BIOME:{} - {}", biomeCB.name(), biomeName);
            }
        }
        map.clear();
    }


    public static void addEnumEnvironment(Registry<LevelStem> registry) {
        int i = World.Environment.values().length;
        for (Entry<ResourceKey<LevelStem>, LevelStem> entry : registry.entrySet()) {
            ResourceKey<LevelStem> key = entry.getKey();
            World.Environment environment1 = environment.get(key);
            if (environment1 == null) {
                String name = MohistDynamEnum.normalizeName(key.location().toString());
                int id = i - 1;
                environment1 = MohistDynamEnum.addEnum(World.Environment.class, name, List.of(Integer.TYPE), List.of(id));
                environment.put(key, environment1);
                environment0.put(environment1, key);
                Youer.LOGGER.debug("Registered forge DimensionType as environment {}", environment1);
                i++;
            }
        }
    }

    public static void addEnumEntity() {
        var registry = BuiltInRegistries.ENTITY_TYPE;
        for (net.minecraft.world.entity.EntityType<?> entity : registry) {
            ResourceLocation resourceLocation = registry.getKey(entity);
            if (resourceLocation == null) continue;

            String entityType = MohistDynamEnum.normalizeName(resourceLocation.toString());
            if (isMods(resourceLocation)) {
                int typeId = entityType.hashCode();
                EntityType bukkitType = MohistDynamEnum.addEnum(EntityType.class, entityType,
                        List.of(String.class, Class.class, Integer.TYPE, Boolean.TYPE),
                        List.of(entityType.toLowerCase(), Entity.class, typeId, false));

                if (bukkitType != null) {
                    bukkitType.hookForgeEntity(resourceLocation, entity);
                }
            } else {
                ServerAPI.entityTypeMap.put(entity, MohistDynamEnum.normalizeName(resourceLocation.getPath()));
            }
        }
        EntityClassLookup.init();
    }

    public static void addEnumAttribute() {
        var registry = BuiltInRegistries.ATTRIBUTE;
        for (Attribute attribute : registry) {
            ResourceLocation resourceLocation = registry.getKey(attribute);
            if (isMods(resourceLocation)) {
                String name = MohistDynamEnum.normalizeName(resourceLocation.getPath());
                org.bukkit.attribute.Attribute ab = MohistDynamEnum.addEnum(org.bukkit.attribute.Attribute.class, name, List.of(String.class), List.of());
                attributemap.put(ab, resourceLocation);
                Youer.LOGGER.debug("Registered forge Attribute as Attribute(Bukkit) {}", ab.name());
            }
        }
    }

    public static void addFluid() {
        var registry = BuiltInRegistries.FLUID;
        for (net.minecraft.world.level.material.Fluid fluidType : registry) {
            ResourceLocation resourceLocation = registry.getKey(fluidType);
            if (isMods(resourceLocation)) {
                String name = MohistDynamEnum.normalizeName(resourceLocation.getPath());
                Fluid fluid = MohistDynamEnum.addEnum(Fluid.class, name);
                Youer.LOGGER.debug("Registered forge Fluid as Fluid(Bukkit) {}", fluid.name());
            }
        }
    }

    public static void addStatistic() {
        var registry = BuiltInRegistries.STAT_TYPE;
        for (StatType<?> statType : registry) {
            ResourceLocation resourceLocation = registry.getKey(statType);
            if (isMods(resourceLocation)) {
                String name = MohistDynamEnum.normalizeName(resourceLocation.getPath());
                Statistic statistic = MohistDynamEnum.addEnum(Statistic.class, name);
                statisticMap.put(statType, statistic);
                Youer.LOGGER.debug("Registered forge StatType as Statistic(Bukkit) {}", statistic.name());
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
                Youer.LOGGER.debug("Registered forge MobCategory as SpawnCategory(Bukkit) {}", spawnCategory);
            }
        }
    }

    private static void addPose() {
        for (Pose pose : Pose.values()) {
            if (pose.ordinal() > 14) {
                org.bukkit.entity.Pose bukkit = MohistDynamEnum.addEnum(org.bukkit.entity.Pose.class, pose.name());
                Youer.LOGGER.debug("Registered forge Pose as Pose(Bukkit) {}", bukkit);
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
            if (isMods(resourceLocation)) {
                String name = MohistDynamEnum.normalizeName(resourceLocation.toString());
                String lookupName = resourceLocation.getPath().toLowerCase(Locale.ROOT);
                int id = i - 1;
                Art art = MohistDynamEnum.addEnum(Art.class, name, List.of(Integer.TYPE, Integer.TYPE, Integer.TYPE), List.of(id, width, height));
                Art.BY_NAME.put(lookupName, art);
                Art.BY_ID.put(id, art);
                Youer.LOGGER.debug("Registered forge PaintingType as Art {}", art);
                i++;
            }
        }
    }

    public static void addModSound() {
        var registry = BuiltInRegistries.SOUND_EVENT;
        for (SoundEvent statType : registry) {
            ResourceLocation resourceLocation = registry.getKey(statType);
            if (isMods(resourceLocation)) {
                String name = resourceLocation.getPath().replace(".", "_").toUpperCase(Locale.ROOT);
                Sound sound = MohistDynamEnum.addEnum(Sound.class, name, List.of(String.class), List.of(resourceLocation.toString()));
                Sound.MODD_SOUNDS.put(statType, sound);
                Youer.LOGGER.debug("Registered mods SoundEvent as Sound(Bukkit) {}", sound.name());
            }
        }
    }

    public static boolean isMods(ResourceLocation resourceLocation) {
        return resourceLocation != null && !resourceLocation.getNamespace().equals(NamespacedKey.MINECRAFT);
    }
}
