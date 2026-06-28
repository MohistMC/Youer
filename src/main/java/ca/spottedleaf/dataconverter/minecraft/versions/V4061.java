package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.nbt.NBTMapType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import java.util.HashMap;
import java.util.Map;

public final class V4061 {

    private static final int VERSION = MCVersions.V24W34A + 1;

    private static final Map<Pair<MapType, MapType>, String> CONVERT_MAP = new HashMap<>();
    static {
        addConversion("trial_chamber/breeze", "{simultaneous_mobs: 1.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:breeze\"}}, weight: 1}], ticks_between_spawn: 20, total_mobs: 2.0f, total_mobs_added_per_player: 1.0f}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 2.0f, total_mobs: 4.0f}");
        addConversion("trial_chamber/melee/husk", "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:husk\"}}, weight: 1}], ticks_between_spawn: 20}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:husk\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_melee\", slot_drop_chances: 0.0f}}, weight: 1}]}");
        addConversion("trial_chamber/melee/spider", "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:spider\"}}, weight: 1}], ticks_between_spawn: 20}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],simultaneous_mobs: 4.0f, total_mobs: 12.0f}");
        addConversion("trial_chamber/melee/zombie", "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:zombie\"}}, weight: 1}], ticks_between_spawn: 20}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],spawn_potentials: [{data: {entity: {id: \"minecraft:zombie\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_melee\", slot_drop_chances: 0.0f}}, weight: 1}]}");
        addConversion("trial_chamber/ranged/poison_skeleton", "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}}, weight: 1}], ticks_between_spawn: 20}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}");
        addConversion("trial_chamber/ranged/skeleton", "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}}, weight: 1}], ticks_between_spawn: 20}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}");
        addConversion("trial_chamber/ranged/stray", "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}}, weight: 1}], ticks_between_spawn: 20}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}");
        addConversion("trial_chamber/slow_ranged/poison_skeleton", "{simultaneous_mobs: 4.0f, simultaneous_mobs_added_per_player: 2.0f, spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}}, weight: 1}], ticks_between_spawn: 160}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}");
        addConversion("trial_chamber/slow_ranged/skeleton", "{simultaneous_mobs: 4.0f, simultaneous_mobs_added_per_player: 2.0f, spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}}, weight: 1}], ticks_between_spawn: 160}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}");
        addConversion("trial_chamber/slow_ranged/stray", "{simultaneous_mobs: 4.0f, simultaneous_mobs_added_per_player: 2.0f, spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}}, weight: 1}], ticks_between_spawn: 160}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}");
        addConversion("trial_chamber/small_melee/baby_zombie", "{simultaneous_mobs: 2.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {IsBaby: 1b, id: \"minecraft:zombie\"}}, weight: 1}], ticks_between_spawn: 20}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {IsBaby: 1b, id: \"minecraft:zombie\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_melee\", slot_drop_chances: 0.0f}}, weight: 1}]}");
        addConversion("trial_chamber/small_melee/cave_spider", "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:cave_spider\"}}, weight: 1}], ticks_between_spawn: 20}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 4.0f, total_mobs: 12.0f}");
        addConversion("trial_chamber/small_melee/silverfish", "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:silverfish\"}}, weight: 1}], ticks_between_spawn: 20}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 4.0f, total_mobs: 12.0f}");
        addConversion("trial_chamber/small_melee/slime", "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {Size: 1, id: \"minecraft:slime\"}}, weight: 3}, {data: {entity: {Size: 2, id: \"minecraft:slime\"}}, weight: 1}], ticks_between_spawn: 20}", "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 4.0f, total_mobs: 12.0f}");
    }

    private static void addConversion(final String keyPath, final String normalsNBT, final String ominoussNBT) {
        final String fullKey = "minecraft:".concat(keyPath);

        final CompoundTag normalNBT = parseNBT(normalsNBT);
        final MapType normalMapType = new NBTMapType(normalNBT);
        final CompoundTag ominousNBT = parseNBT(ominoussNBT);

        final CompoundTag ominousMerged = normalNBT.copy().merge(ominousNBT);

        CONVERT_MAP.put(Pair.of(normalMapType, new NBTMapType(ominousNBT)), fullKey);
        CONVERT_MAP.put(Pair.of(normalMapType, new NBTMapType(ominousMerged)), fullKey);
        CONVERT_MAP.put(Pair.of(normalMapType, new NBTMapType(removeDefaults(ominousMerged.copy()))), fullKey);
    }

    private static CompoundTag parseNBT(final String sNBT) {
        try {
            return TagParser.parseCompoundFully(sNBT);
        } catch (final CommandSyntaxException ex) {
            throw new IllegalArgumentException("Failed to parse NBT: " + sNBT, ex);
        }
    }

    private static CompoundTag removeDefaults(final CompoundTag config) {
        if (config.getIntOr("spawn_range", 0) == 4) {
            config.remove("spawn_range");
        }

        if (config.getFloatOr("total_mobs", 0.0F) == 6.0F) {
            config.remove("total_mobs");
        }

        if (config.getFloatOr("simultaneous_mobs", 0.0F) == 2.0F) {
            config.remove("simultaneous_mobs");
        }

        if (config.getFloatOr("total_mobs_added_per_player", 0.0F) == 2.0F) {
            config.remove("total_mobs_added_per_player");
        }

        if (config.getFloatOr("simultaneous_mobs_added_per_player", 0.0F) == 1.0F) {
            config.remove("simultaneous_mobs_added_per_player");
        }

        if (config.getIntOr("ticks_between_spawn", 0) == 40) {
            config.remove("ticks_between_spawn");
        }

        return config;
    }

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:trial_spawner", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType normalConfig = data.getMap("normal_config");
                final MapType ominousConfig = data.getMap("ominous_config");

                if (normalConfig == null || ominousConfig == null) {
                    return null;
                }

                final String newKey = CONVERT_MAP.get(new Pair<>(normalConfig, ominousConfig));
                if (newKey == null) {
                    return null;
                }

                data.setString("normal_config", newKey.concat("/normal"));
                data.setString("ominous_config", newKey.concat("/ominous"));

                return null;
            }
        });
    }

    private V4061() {}
}
