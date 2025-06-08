package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.util.ComponentUtils;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;
import ca.spottedleaf.dataconverter.types.MapType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class V3825 {

    private static final int VERSION = MCVersions.V24W12A + 1;

    private static final Set<String> BANNER_NAMES = new HashSet<>(
            Arrays.asList(
                    "block.minecraft.ominous_banner"
            )
    );
    private static final Set<String> MAP_NAMES = new HashSet<>(
            Arrays.asList(
                    "filled_map.buried_treasure",
                    "filled_map.explorer_jungle",
                    "filled_map.explorer_swamp",
                    "filled_map.mansion",
                    "filled_map.monument",
                    "filled_map.trial_chambers",
                    "filled_map.village_desert",
                    "filled_map.village_plains",
                    "filled_map.village_savanna",
                    "filled_map.village_snowy",
                    "filled_map.village_taiga"
            )
    );

    public static void register() {
        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(VERSION) {
            private static void convertName(final MapType<String> components, final Set<String> standardNames) {
                final String customName = components.getString("minecraft:custom_name");
                if (customName == null) {
                    return;
                }

                final String translation = ComponentUtils.retrieveTranslationString(customName);
                if (translation == null) {
                    return;
                }

                if (standardNames.contains(translation)) {
                    components.remove("minecraft:custom_name");
                    components.setString("minecraft:item_name", customName);
                }
            }

            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> components = data.getMap("components");
                if (components == null) {
                    return null;
                }

                final String id = data.getString("id");
                if (id == null) {
                    return null;
                }

                switch (id) {
                    case "minecraft:white_banner": {
                        convertName(components, BANNER_NAMES);
                        break;
                    }
                    case "minecraft:filled_map": {
                        convertName(components, MAP_NAMES);
                        break;
                    }
                }

                return null;
            }
        });
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:banner", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final String customName = data.getString("CustomName");
                if (customName == null || !"block.minecraft.ominous_banner".equals(ComponentUtils.retrieveTranslationString(customName))) {
                    return null;
                }

                data.remove("CustomName");

                final MapType<String> components = data.getOrCreateMap("components");

                components.setString("minecraft:item_name", customName);
                components.setMap("minecraft:hide_additional_tooltip", components.getTypeUtil().createEmptyMap());

                return null;
            }
        });
        // DFU does not change the schema, even though it moves spawn_potentials
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:trial_spawner", (final MapType<String> data, final long fromVersion, final long toVersion) -> {
            final MapType<String> normalConfig = data.getMap("normal_config");
            if (normalConfig != null) {
                WalkerUtils.convertListPath(MCTypeRegistry.ENTITY, normalConfig, "spawn_potentials", "data", "entity", fromVersion, toVersion);
            }
            final MapType<String> ominousConfig = data.getMap("ominous_config");
            if (ominousConfig != null) {
                WalkerUtils.convertListPath(MCTypeRegistry.ENTITY, ominousConfig, "spawn_potentials", "data", "entity", fromVersion, toVersion);
            }

            WalkerUtils.convert(MCTypeRegistry.ENTITY, data.getMap("spawn_data"), "entity", fromVersion, toVersion);
            return null;
        });
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:trial_spawner", new DataConverter<>(VERSION) {
            private static final String[] NORMAL_CONFIG_KEYS = new String[] {
                    "spawn_range",
                    "total_mobs",
                    "simultaneous_mobs",
                    "total_mobs_added_per_player",
                    "simultaneous_mobs_added_per_player",
                    "ticks_between_spawn",
                    "spawn_potentials",
                    "loot_tables_to_eject",
                    "items_to_drop_when_ominous"
            };

            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> normalConfig = data.getTypeUtil().createEmptyMap();

                for (final String normalKey : NORMAL_CONFIG_KEYS) {
                    final Object normalValue = data.getGeneric(normalKey);
                    if (normalValue != null) {
                        data.remove(normalKey);
                        normalConfig.setGeneric(normalKey, normalValue);
                    }
                }

                if (!normalConfig.isEmpty()) {
                    data.setMap("normal_config", normalConfig);
                }

                return null;
            }
        });

        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:ominous_item_spawner", new DataWalkerItems("item"));
    }

    private V3825() {}
}
