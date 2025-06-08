package ca.spottedleaf.dataconverter.minecraft.converters.stats;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.HelperBlockFlatteningV1450;
import ca.spottedleaf.dataconverter.minecraft.converters.itemstack.ConverterFlattenItemStack;
import ca.spottedleaf.dataconverter.minecraft.versions.V1451;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.Types;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ConverterFlattenStats {

    private static final int VERSION = MCVersions.V17W47A;
    private static final int VERSION_STEP = 6;

    private static final Set<String> SPECIAL_OBJECTIVE_CRITERIA = new HashSet<>(
            Set.of(
                    "dummy",
                    "trigger",
                    "deathCount",
                    "playerKillCount",
                    "totalKillCount",
                    "health",
                    "food",
                    "air",
                    "armor",
                    "xp",
                    "level",
                    "killedByTeam.aqua",
                    "killedByTeam.black",
                    "killedByTeam.blue",
                    "killedByTeam.dark_aqua",
                    "killedByTeam.dark_blue",
                    "killedByTeam.dark_gray",
                    "killedByTeam.dark_green",
                    "killedByTeam.dark_purple",
                    "killedByTeam.dark_red",
                    "killedByTeam.gold",
                    "killedByTeam.gray",
                    "killedByTeam.green",
                    "killedByTeam.light_purple",
                    "killedByTeam.red",
                    "killedByTeam.white",
                    "killedByTeam.yellow",
                    "teamkill.aqua",
                    "teamkill.black",
                    "teamkill.blue",
                    "teamkill.dark_aqua",
                    "teamkill.dark_blue",
                    "teamkill.dark_gray",
                    "teamkill.dark_green",
                    "teamkill.dark_purple",
                    "teamkill.dark_red",
                    "teamkill.gold",
                    "teamkill.gray",
                    "teamkill.green",
                    "teamkill.light_purple",
                    "teamkill.red",
                    "teamkill.white",
                    "teamkill.yellow"
            )
    );

    private static final Set<String> SKIP = new HashSet<>(
            ImmutableSet.<String>builder()
                    .add("stat.craftItem.minecraft.spawn_egg")
                    .add("stat.useItem.minecraft.spawn_egg")
                    .add("stat.breakItem.minecraft.spawn_egg")
                    .add("stat.pickup.minecraft.spawn_egg")
                    .add("stat.drop.minecraft.spawn_egg")
                    .build()
    );

    private static final Map<String, String> CUSTOM_MAP = new HashMap<>(
            ImmutableMap.<String, String>builder()
                    .put("stat.leaveGame", "minecraft:leave_game")
                    .put("stat.playOneMinute", "minecraft:play_one_minute")
                    .put("stat.timeSinceDeath", "minecraft:time_since_death")
                    .put("stat.sneakTime", "minecraft:sneak_time")
                    .put("stat.walkOneCm", "minecraft:walk_one_cm")
                    .put("stat.crouchOneCm", "minecraft:crouch_one_cm")
                    .put("stat.sprintOneCm", "minecraft:sprint_one_cm")
                    .put("stat.swimOneCm", "minecraft:swim_one_cm")
                    .put("stat.fallOneCm", "minecraft:fall_one_cm")
                    .put("stat.climbOneCm", "minecraft:climb_one_cm")
                    .put("stat.flyOneCm", "minecraft:fly_one_cm")
                    .put("stat.diveOneCm", "minecraft:dive_one_cm")
                    .put("stat.minecartOneCm", "minecraft:minecart_one_cm")
                    .put("stat.boatOneCm", "minecraft:boat_one_cm")
                    .put("stat.pigOneCm", "minecraft:pig_one_cm")
                    .put("stat.horseOneCm", "minecraft:horse_one_cm")
                    .put("stat.aviateOneCm", "minecraft:aviate_one_cm")
                    .put("stat.jump", "minecraft:jump")
                    .put("stat.drop", "minecraft:drop")
                    .put("stat.damageDealt", "minecraft:damage_dealt")
                    .put("stat.damageTaken", "minecraft:damage_taken")
                    .put("stat.deaths", "minecraft:deaths")
                    .put("stat.mobKills", "minecraft:mob_kills")
                    .put("stat.animalsBred", "minecraft:animals_bred")
                    .put("stat.playerKills", "minecraft:player_kills")
                    .put("stat.fishCaught", "minecraft:fish_caught")
                    .put("stat.talkedToVillager", "minecraft:talked_to_villager")
                    .put("stat.tradedWithVillager", "minecraft:traded_with_villager")
                    .put("stat.cakeSlicesEaten", "minecraft:eat_cake_slice")
                    .put("stat.cauldronFilled", "minecraft:fill_cauldron")
                    .put("stat.cauldronUsed", "minecraft:use_cauldron")
                    .put("stat.armorCleaned", "minecraft:clean_armor")
                    .put("stat.bannerCleaned", "minecraft:clean_banner")
                    .put("stat.brewingstandInteraction", "minecraft:interact_with_brewingstand")
                    .put("stat.beaconInteraction", "minecraft:interact_with_beacon")
                    .put("stat.dropperInspected", "minecraft:inspect_dropper")
                    .put("stat.hopperInspected", "minecraft:inspect_hopper")
                    .put("stat.dispenserInspected", "minecraft:inspect_dispenser")
                    .put("stat.noteblockPlayed", "minecraft:play_noteblock")
                    .put("stat.noteblockTuned", "minecraft:tune_noteblock")
                    .put("stat.flowerPotted", "minecraft:pot_flower")
                    .put("stat.trappedChestTriggered", "minecraft:trigger_trapped_chest")
                    .put("stat.enderchestOpened", "minecraft:open_enderchest")
                    .put("stat.itemEnchanted", "minecraft:enchant_item")
                    .put("stat.recordPlayed", "minecraft:play_record")
                    .put("stat.furnaceInteraction", "minecraft:interact_with_furnace")
                    .put("stat.craftingTableInteraction", "minecraft:interact_with_crafting_table")
                    .put("stat.chestOpened", "minecraft:open_chest")
                    .put("stat.sleepInBed", "minecraft:sleep_in_bed")
                    .put("stat.shulkerBoxOpened", "minecraft:open_shulker_box")
                    .build()
    );

    private static final String BLOCK_KEY = "stat.mineBlock";
    private static final String NEW_BLOCK_KEY = "minecraft:mined";

    private static final Map<String, String> ITEM_KEYS = new HashMap<>(
            ImmutableMap.<String, String>builder()
                    .put("stat.craftItem", "minecraft:crafted")
                    .put("stat.useItem", "minecraft:used")
                    .put("stat.breakItem", "minecraft:broken")
                    .put("stat.pickup", "minecraft:picked_up")
                    .put("stat.drop", "minecraft:dropped")
                    .build()
    );

    private static final Map<String, String> ENTITY_KEYS = new HashMap<>(
            ImmutableMap.<String, String>builder()
                    .put("stat.entityKilledBy", "minecraft:killed_by")
                    .put("stat.killEntity", "minecraft:killed")
                    .build()
    );

    private static final Map<String, String> ENTITIES = new HashMap<>(
            ImmutableMap.<String, String>builder()
                    .put("Bat", "minecraft:bat")
                    .put("Blaze", "minecraft:blaze")
                    .put("CaveSpider", "minecraft:cave_spider")
                    .put("Chicken", "minecraft:chicken")
                    .put("Cow", "minecraft:cow")
                    .put("Creeper", "minecraft:creeper")
                    .put("Donkey", "minecraft:donkey")
                    .put("ElderGuardian", "minecraft:elder_guardian")
                    .put("Enderman", "minecraft:enderman")
                    .put("Endermite", "minecraft:endermite")
                    .put("EvocationIllager", "minecraft:evocation_illager")
                    .put("Ghast", "minecraft:ghast")
                    .put("Guardian", "minecraft:guardian")
                    .put("Horse", "minecraft:horse")
                    .put("Husk", "minecraft:husk")
                    .put("Llama", "minecraft:llama")
                    .put("LavaSlime", "minecraft:magma_cube")
                    .put("MushroomCow", "minecraft:mooshroom")
                    .put("Mule", "minecraft:mule")
                    .put("Ozelot", "minecraft:ocelot")
                    .put("Parrot", "minecraft:parrot")
                    .put("Pig", "minecraft:pig")
                    .put("PolarBear", "minecraft:polar_bear")
                    .put("Rabbit", "minecraft:rabbit")
                    .put("Sheep", "minecraft:sheep")
                    .put("Shulker", "minecraft:shulker")
                    .put("Silverfish", "minecraft:silverfish")
                    .put("SkeletonHorse", "minecraft:skeleton_horse")
                    .put("Skeleton", "minecraft:skeleton")
                    .put("Slime", "minecraft:slime")
                    .put("Spider", "minecraft:spider")
                    .put("Squid", "minecraft:squid")
                    .put("Stray", "minecraft:stray")
                    .put("Vex", "minecraft:vex")
                    .put("Villager", "minecraft:villager")
                    .put("VindicationIllager", "minecraft:vindication_illager")
                    .put("Witch", "minecraft:witch")
                    .put("WitherSkeleton", "minecraft:wither_skeleton")
                    .put("Wolf", "minecraft:wolf")
                    .put("ZombieHorse", "minecraft:zombie_horse")
                    .put("PigZombie", "minecraft:zombie_pigman")
                    .put("ZombieVillager", "minecraft:zombie_villager")
                    .put("Zombie", "minecraft:zombie")
                    .build()
    );

    private static final String NEW_CUSTOM_KEY = "minecraft:custom";

    private ConverterFlattenStats() {}

    private static String upgradeItem(final String itemName) {
        return ConverterFlattenItemStack.flattenItem(itemName, 0);
    }

    private static String upgradeBlock(final String block) {
        return HelperBlockFlatteningV1450.getNewBlockName(block);
    }

    private static record StatType(String category, String key) {}

    private static StatType convertLegacyKey(final String key) {
        if (SKIP.contains(key)) {
            return null;
        }

        final String custom = CUSTOM_MAP.get(key);
        if (custom != null) {
            return new StatType(NEW_CUSTOM_KEY, custom);
        }

        final int i = StringUtils.ordinalIndexOf(key, ".", 2);
        if (i < 0) {
            return null;
        }

        final String stat = key.substring(0, i);

        if (BLOCK_KEY.equals(stat)) {
            return new StatType(NEW_BLOCK_KEY, upgradeBlock(key.substring(i + 1).replace('.', ':')));
        }

        final String itemStat = ITEM_KEYS.get(stat);

        if (itemStat != null) {
            final String itemId = key.substring(i + 1).replace('.', ':');
            final String flattenedItem = upgradeItem(itemId);

            return new StatType(itemStat, flattenedItem == null ? itemId : flattenedItem);
        }

        final String entityStat = ENTITY_KEYS.get(stat);
        if (entityStat != null) {
            final String entityId = key.substring(i + 1).replace('.', ':');

            return new StatType(entityStat, ENTITIES.getOrDefault(entityId, entityId));
        }

        return null;
    }

    public static DataConverter<MapType<String>, MapType<String>> makeStatsConverter() {
        return new DataConverter<>(VERSION, VERSION_STEP) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> stats = Types.NBT.createEmptyMap();

                for (final String statKey : data.keys()) {
                    final Number value = data.getNumber(statKey);
                    if (value == null) {
                        continue;
                    }

                    final StatType converted = convertLegacyKey(statKey);

                    if (converted == null) {
                        continue;
                    }

                    stats.getOrCreateMap(converted.category()).setGeneric(converted.key(), value);
                }

                data.clear();
                data.setMap("stats", stats);

                return null;
            }
        };
    }

    public static DataConverter<MapType<String>, MapType<String>> makeObjectiveConverter() {
        return new DataConverter<>(VERSION, VERSION_STEP) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                convertCriteriaName(data, "CriteriaName");

                // We also need to update CriteriaType that is created by the data hook in V1451,
                // otherwise that data hook will overwrite our CriteriaName
                final MapType<String> criteriaType = data.getMap("CriteriaType");
                if (criteriaType != null) {
                    if ("_special".equals(criteriaType.getString("type"))) {
                        convertCriteriaName(criteriaType, "id");
                    }
                }

                return null;
            }

            private void convertCriteriaName(final MapType<String> data, final String key) {
                final String criteriaName = data.getString(key);

                if (criteriaName == null) {
                    return;
                }

                if (SPECIAL_OBJECTIVE_CRITERIA.contains(criteriaName)) {
                    return;
                }

                final StatType converted = convertLegacyKey(criteriaName);
                data.setString(key, converted == null ? "dummy" : V1451.packWithDot(converted.category()) + ":" + V1451.packWithDot(converted.key()));
            }
        };
    }
}
