package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class V3568 {

    private static final int VERSION = MCVersions.V23W31A + 1;

    private static final String[] EFFECT_ID_MAP = new String[34];
    static {
        EFFECT_ID_MAP[1] = "minecraft:speed";
        EFFECT_ID_MAP[2] = "minecraft:slowness";
        EFFECT_ID_MAP[3] = "minecraft:haste";
        EFFECT_ID_MAP[4] = "minecraft:mining_fatigue";
        EFFECT_ID_MAP[5] = "minecraft:strength";
        EFFECT_ID_MAP[6] = "minecraft:instant_health";
        EFFECT_ID_MAP[7] = "minecraft:instant_damage";
        EFFECT_ID_MAP[8] = "minecraft:jump_boost";
        EFFECT_ID_MAP[9] = "minecraft:nausea";
        EFFECT_ID_MAP[10] = "minecraft:regeneration";
        EFFECT_ID_MAP[11] = "minecraft:resistance";
        EFFECT_ID_MAP[12] = "minecraft:fire_resistance";
        EFFECT_ID_MAP[13] = "minecraft:water_breathing";
        EFFECT_ID_MAP[14] = "minecraft:invisibility";
        EFFECT_ID_MAP[15] = "minecraft:blindness";
        EFFECT_ID_MAP[16] = "minecraft:night_vision";
        EFFECT_ID_MAP[17] = "minecraft:hunger";
        EFFECT_ID_MAP[18] = "minecraft:weakness";
        EFFECT_ID_MAP[19] = "minecraft:poison";
        EFFECT_ID_MAP[20] = "minecraft:wither";
        EFFECT_ID_MAP[21] = "minecraft:health_boost";
        EFFECT_ID_MAP[22] = "minecraft:absorption";
        EFFECT_ID_MAP[23] = "minecraft:saturation";
        EFFECT_ID_MAP[24] = "minecraft:glowing";
        EFFECT_ID_MAP[25] = "minecraft:levitation";
        EFFECT_ID_MAP[26] = "minecraft:luck";
        EFFECT_ID_MAP[27] = "minecraft:unluck";
        EFFECT_ID_MAP[28] = "minecraft:slow_falling";
        EFFECT_ID_MAP[29] = "minecraft:conduit_power";
        EFFECT_ID_MAP[30] = "minecraft:dolphins_grace";
        EFFECT_ID_MAP[31] = "minecraft:bad_omen";
        EFFECT_ID_MAP[32] = "minecraft:hero_of_the_village";
        EFFECT_ID_MAP[33] = "minecraft:darkness";
    }
    private static final Set<String> EFFECT_ITEMS =
            new HashSet<>(
                    Set.of(
                            "minecraft:potion",
                            "minecraft:splash_potion",
                            "minecraft:lingering_potion",
                            "minecraft:tipped_arrow"
                    )
            );

    private static String readLegacyEffect(final MapType<String> data, final String path) {
        final Number id = data.getNumber(path);
        if (id == null) {
            return null;
        }

        final int castedId = id.intValue();
        return castedId >= 0 && castedId < EFFECT_ID_MAP.length ? EFFECT_ID_MAP[castedId] : null;
    }

    private static void convertLegacyEffect(final MapType<String> data, final String legacyPath, final String newPath) {
        final Number id = data.getNumber(legacyPath);
        data.remove(legacyPath);

        if (id == null) {
            return;
        }

        final int castedId = id.intValue();
        final String newId = castedId >= 0 && castedId < EFFECT_ID_MAP.length ? EFFECT_ID_MAP[castedId] : null;

        if (newId == null) {
            return;
        }

        data.setString(newPath, newId);
    }

    private static final Map<String, String> MOB_EFFECT_RENAMES = new HashMap<>();
    static {
        MOB_EFFECT_RENAMES.put("Ambient", "ambient");
        MOB_EFFECT_RENAMES.put("Amplifier", "amplifier");
        MOB_EFFECT_RENAMES.put("Duration", "duration");
        MOB_EFFECT_RENAMES.put("ShowParticles", "show_particles");
        MOB_EFFECT_RENAMES.put("ShowIcon", "show_icon");
        MOB_EFFECT_RENAMES.put("FactorCalculationData", "factor_calculation_data");
        MOB_EFFECT_RENAMES.put("HiddenEffect", "hidden_effect");
    }

    private static void convertMobEffect(final MapType<String> mobEffect) {
        if (mobEffect == null) {
            return;
        }

        convertLegacyEffect(mobEffect, "Id", "id");

        for (final Map.Entry<String, String> rename : MOB_EFFECT_RENAMES.entrySet()) {
            RenameHelper.renameSingle(mobEffect, rename.getKey(), rename.getValue());
        }

        convertMobEffect(mobEffect.getMap("hidden_effect"));
    }

    private static void convertMobEffectList(final MapType<String> data, final String oldPath, final String newPath) {
        final ListType effects = data.getList(oldPath, ObjectType.MAP);
        if (effects == null) {
            return;
        }

        for (int i = 0, len = effects.size(); i < len; ++i) {
            convertMobEffect(effects.getMap(i));
        }

        data.remove(oldPath);
        data.setList(newPath, effects);
    }

    private static void removeAndSet(final MapType<String> data, final String toRemovePath,
                                     final String toSetPath, final Object toSet) {
        data.remove(toRemovePath);
        if (toSet != null) {
            data.setGeneric(toSetPath, toSet);
        }
    }

    private static void updateSuspiciousStew(final MapType<String> from, final MapType<String> into) {
        removeAndSet(into, "EffectId", "id", readLegacyEffect(from, "EffectId"));
        removeAndSet(into, "EffectDuration", "duration", from.getGeneric("EffectDuration"));
    }

    public static void register() {
        final DataConverter<MapType<String>, MapType<String>> beaconConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                convertLegacyEffect(data, "Primary", "primary_effect");
                convertLegacyEffect(data, "Secondary", "secondary_effect");

                return null;
            }
        };

        final DataConverter<MapType<String>, MapType<String>> mooshroomConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> newEffect = data.getTypeUtil().createEmptyMap();
                updateSuspiciousStew(data, newEffect);

                data.remove("EffectId");
                data.remove("EffectDuration");

                if (!newEffect.isEmpty()) {
                    final ListType stewEffects = data.getTypeUtil().createEmptyList();
                    data.setList("stew_effects", stewEffects);

                    stewEffects.addMap(newEffect);
                }

                return null;
            }
        };
        final DataConverter<MapType<String>, MapType<String>> arrowConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                convertMobEffectList(data, "CustomPotionEffects", "custom_potion_effects");
                return null;
            }
        };
        final DataConverter<MapType<String>, MapType<String>> areaEffectCloudConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                convertMobEffectList(data, "Effects", "effects");
                return null;
            }
        };
        final DataConverter<MapType<String>, MapType<String>> livingEntityConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                convertMobEffectList(data, "ActiveEffects", "active_effects");
                return null;
            }
        };

        final DataConverter<MapType<String>, MapType<String>> itemConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> root, final long sourceVersion, final long toVersion) {
                final String id = root.getString("id");

                final MapType<String> tag = root.getMap("tag");

                if (tag == null) {
                    return null;
                }

                if ("minecraft:suspicious_stew".equals(id)) {
                    RenameHelper.renameSingle(tag, "Effects", "effects");

                    final ListType effects = tag.getList("effects", ObjectType.MAP);

                    if (effects != null) {
                        for (int i = 0, len = effects.size(); i < len; ++i) {
                            final MapType<String> effect = effects.getMap(i);
                            updateSuspiciousStew(effect, effect);
                        }
                    }

                    return null;
                }

                if (EFFECT_ITEMS.contains(id)) {
                    convertMobEffectList(tag, "CustomPotionEffects", "custom_potion_effects");
                    return null;
                }

                return null;
            }
        };

        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:beacon", beaconConverter);

        MCTypeRegistry.ENTITY.addConverterForId("minecraft:mooshroom", mooshroomConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:arrow", arrowConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:area_effect_cloud", areaEffectCloudConverter);
        MCTypeRegistry.ENTITY.addStructureConverter(livingEntityConverter);

        MCTypeRegistry.PLAYER.addStructureConverter(livingEntityConverter);

        MCTypeRegistry.ITEM_STACK.addStructureConverter(itemConverter);
    }

    private V3568() {}
}
