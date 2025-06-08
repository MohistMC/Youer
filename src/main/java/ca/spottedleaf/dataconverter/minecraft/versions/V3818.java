package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.converters.datatypes.DataWalker;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.custom.V3818_Commands;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.converters.itemstack.ConverterItemStackToDataComponents;
import ca.spottedleaf.dataconverter.minecraft.converters.particle.ConverterParticleToNBT;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import ca.spottedleaf.dataconverter.types.Types;
import java.util.HashMap;
import java.util.Map;

public final class V3818 {

    private static final int VERSION = MCVersions.V24W07A + 1;

    private static final String[] BANNER_COLOURS = new String[] {
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black",
    };

    public static String getBannerColour(final int id) {
        return id >= 0 && id < BANNER_COLOURS.length ? BANNER_COLOURS[id] : BANNER_COLOURS[0];
    }

    public static void register() {
        // Step 0
        // Note: No breakpoint needed, nothing nests hotbar
        MCTypeRegistry.HOTBAR.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                for (final String key : data.keys()) {
                    final ListType itemList = data.getList(key, ObjectType.MAP);
                    if (itemList != null) {
                        for (int i = 0, len = itemList.size(); i < len; ++i) {
                            final MapType<String> item = itemList.getMap(i);

                            final String id = item.getString("id");
                            final int count = item.getInt("Count");

                            if ("minecraft:air".equals(id) || count <= 0) {
                                itemList.setMap(i, item.getTypeUtil().createEmptyMap());
                            }
                        }
                    }
                }

                return null;
            }
        });

        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:beehive", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                RenameHelper.renameSingle(data, "Bees", "bees");

                final ListType bees = data.getList("bees", ObjectType.MAP);
                if (bees != null) {
                    for (int i = 0, len = bees.size(); i < len; ++i) {
                        final MapType<String> bee = bees.getMap(i);

                        RenameHelper.renameSingle(bee, "EntityData", "entity_data");
                        RenameHelper.renameSingle(bee, "TicksInHive", "ticks_in_hive");
                        RenameHelper.renameSingle(bee, "MinOccupationTicks", "min_ticks_in_hive");
                    }
                }

                return null;
            }
        });
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:beehive", (final MapType<String> root, final long fromVersion, final long toVersion) -> {
            WalkerUtils.convertListPath(MCTypeRegistry.ENTITY, root, "bees", "entity_data", fromVersion, toVersion);

            return null;
        });

        // Step 1
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:banner", new DataConverter<>(VERSION, 1) {
            private static final Map<String, String> PATTERN_UPDATE = new HashMap<>();
            static {
                PATTERN_UPDATE.put("b", "minecraft:base");
                PATTERN_UPDATE.put("bl", "minecraft:square_bottom_left");
                PATTERN_UPDATE.put("br", "minecraft:square_bottom_right");
                PATTERN_UPDATE.put("tl", "minecraft:square_top_left");
                PATTERN_UPDATE.put("tr", "minecraft:square_top_right");
                PATTERN_UPDATE.put("bs", "minecraft:stripe_bottom");
                PATTERN_UPDATE.put("ts", "minecraft:stripe_top");
                PATTERN_UPDATE.put("ls", "minecraft:stripe_left");
                PATTERN_UPDATE.put("rs", "minecraft:stripe_right");
                PATTERN_UPDATE.put("cs", "minecraft:stripe_center");
                PATTERN_UPDATE.put("ms", "minecraft:stripe_middle");
                PATTERN_UPDATE.put("drs", "minecraft:stripe_downright");
                PATTERN_UPDATE.put("dls", "minecraft:stripe_downleft");
                PATTERN_UPDATE.put("ss", "minecraft:small_stripes");
                PATTERN_UPDATE.put("cr", "minecraft:cross");
                PATTERN_UPDATE.put("sc", "minecraft:straight_cross");
                PATTERN_UPDATE.put("bt", "minecraft:triangle_bottom");
                PATTERN_UPDATE.put("tt", "minecraft:triangle_top");
                PATTERN_UPDATE.put("bts", "minecraft:triangles_bottom");
                PATTERN_UPDATE.put("tts", "minecraft:triangles_top");
                PATTERN_UPDATE.put("ld", "minecraft:diagonal_left");
                PATTERN_UPDATE.put("rd", "minecraft:diagonal_up_right");
                PATTERN_UPDATE.put("lud", "minecraft:diagonal_up_left");
                PATTERN_UPDATE.put("rud", "minecraft:diagonal_right");
                PATTERN_UPDATE.put("mc", "minecraft:circle");
                PATTERN_UPDATE.put("mr", "minecraft:rhombus");
                PATTERN_UPDATE.put("vh", "minecraft:half_vertical");
                PATTERN_UPDATE.put("hh", "minecraft:half_horizontal");
                PATTERN_UPDATE.put("vhr", "minecraft:half_vertical_right");
                PATTERN_UPDATE.put("hhb", "minecraft:half_horizontal_bottom");
                PATTERN_UPDATE.put("bo", "minecraft:border");
                PATTERN_UPDATE.put("cbo", "minecraft:curly_border");
                PATTERN_UPDATE.put("gra", "minecraft:gradient");
                PATTERN_UPDATE.put("gru", "minecraft:gradient_up");
                PATTERN_UPDATE.put("bri", "minecraft:bricks");
                PATTERN_UPDATE.put("glb", "minecraft:globe");
                PATTERN_UPDATE.put("cre", "minecraft:creeper");
                PATTERN_UPDATE.put("sku", "minecraft:skull");
                PATTERN_UPDATE.put("flo", "minecraft:flower");
                PATTERN_UPDATE.put("moj", "minecraft:mojang");
                PATTERN_UPDATE.put("pig", "minecraft:piglin");
            }

            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final ListType patterns = data.getList("Patterns", ObjectType.MAP);
                if (patterns != null) {
                    for (int i = 0, len = patterns.size(); i < len; ++i) {
                        final MapType<String> pattern = patterns.getMap(i);

                        final String patternName = pattern.getString("Pattern");
                        if (patternName != null) {
                            final String renamed = PATTERN_UPDATE.get(patternName);
                            if (renamed != null) {
                                pattern.setString("Pattern", renamed);
                            }
                        }
                        RenameHelper.renameSingle(pattern, "Pattern", "pattern");

                        final String newColour = getBannerColour(pattern.getInt("Color"));
                        pattern.setString("Color", newColour);
                        RenameHelper.renameSingle(pattern, "Color", "color");
                    }
                }
                RenameHelper.renameSingle(data, "Patterns", "patterns");

                return null;
            }
        });

        // Step 2
        // Note: there is nothing after the previous breakpoint (1.19.4) that reads nested entity item
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:arrow", new DataConverter<>(VERSION, 2) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final Object potion = data.getGeneric("Potion");
                final Object customPotionEffects = data.getGeneric("custom_potion_effects");
                final Object color = data.getGeneric("Color");

                if (potion == null && customPotionEffects == null && color == null) {
                    return null;
                }

                data.remove("Potion");
                data.remove("custom_potion_effects");
                data.remove("Color");

                final MapType<String> item = data.getMap("item");
                if (item == null) {
                    return null;
                }

                final MapType<String> tag = item.getOrCreateMap("tag");

                if (potion != null) {
                    tag.setGeneric("Potion", potion);
                }
                if (customPotionEffects != null) {
                    tag.setGeneric("custom_potion_effects", customPotionEffects);
                }
                if (color != null) {
                    tag.setGeneric("CustomPotionColor", color);
                }

                return null;
            }
        });

        // Step 3
        MCTypeRegistry.DATA_COMPONENTS.addStructureWalker(VERSION, 3, new DataWalker<>() {
            private static void walkBlockPredicates(final MapType<String> root, final long fromVersion, final long toVersion) {
                if (root.hasKey("blocks", ObjectType.STRING)) {
                    WalkerUtils.convert(MCTypeRegistry.BLOCK_NAME, root, "blocks", fromVersion, toVersion);
                } else if (root.hasKey("blocks", ObjectType.LIST)) {
                    WalkerUtils.convertList(MCTypeRegistry.BLOCK_NAME, root, "blocks", fromVersion, toVersion);
                }
            }

            @Override
            public MapType<String> walk(final MapType<String> root, final long fromVersion, final long toVersion) {
                WalkerUtils.convertListPath(MCTypeRegistry.ENTITY, root, "minecraft:bees", "entity_data", fromVersion, toVersion);

                WalkerUtils.convert(MCTypeRegistry.TILE_ENTITY, root, "minecraft:block_entity_data", fromVersion, toVersion);
                WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, root, "minecraft:bundle_contents", fromVersion, toVersion);

                final MapType<String> canBreak = root.getMap("minecraft:can_break");
                if (canBreak != null) {
                    final ListType predicates = canBreak.getList("predicates", ObjectType.MAP);
                    if (predicates != null) {
                        for (int i = 0, len = predicates.size(); i < len; ++i) {
                            walkBlockPredicates(predicates.getMap(i), fromVersion, toVersion);
                        }
                    }
                    // Not handled by DFU: simple encoding does not require "predicates"
                    walkBlockPredicates(canBreak, fromVersion, toVersion);
                }

                final MapType<String> canPlaceOn = root.getMap("minecraft:can_place_on");
                if (canPlaceOn != null) {
                    final ListType predicates = canPlaceOn.getList("predicates", ObjectType.MAP);
                    if (predicates != null) {
                        for (int i = 0, len = predicates.size(); i < len; ++i) {
                            walkBlockPredicates(predicates.getMap(i), fromVersion, toVersion);
                        }
                    }
                    // Not handled by DFU: simple encoding does not require "predicates"
                    walkBlockPredicates(canPlaceOn, fromVersion, toVersion);
                }

                WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, root, "minecraft:charged_projectiles", fromVersion, toVersion);
                WalkerUtils.convertListPath(MCTypeRegistry.ITEM_STACK, root, "minecraft:container", "item", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ENTITY, root, "minecraft:entity_data", fromVersion, toVersion);
                WalkerUtils.convertList(MCTypeRegistry.ITEM_NAME, root, "minecraft:pot_decorations", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, root.getMap("minecraft:food"), "using_converts_to", fromVersion, toVersion);

                return null;
            }
        });

        // Step 4
        MCTypeRegistry.PARTICLE.addStructureConverter(new DataConverter<>(VERSION, 4) {
            @Override
            public MapType<String> convert(final Object input, final long sourceVersion, final long toVersion) {
                if (!(input instanceof String flat)) {
                    return null;
                }

                return ConverterParticleToNBT.convert(flat, Types.NBT);
            }
        });

        MCTypeRegistry.PARTICLE.addStructureWalker(VERSION, 4, (final Object input, final long fromVersion, final long toVersion) -> {
            if (!(input instanceof MapType<?>)) {
                return null;
            }

            final MapType<String> root = (MapType<String>)input;

            WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, root, "item", fromVersion, toVersion);
            WalkerUtils.convert(MCTypeRegistry.BLOCK_STATE, root, "block_state", fromVersion, toVersion);

            return null;
        });

        // Step 5
        // Note: needs breakpoint, reads nested tile entity data
        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(VERSION, 5) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                return ConverterItemStackToDataComponents.convertItem(data);
            }
        });

        MCTypeRegistry.ITEM_STACK.addStructureWalker(VERSION, 5, (final MapType<String> root, final long fromVersion, final long toVersion) -> {
            WalkerUtils.convert(MCTypeRegistry.ITEM_NAME, root, "id", fromVersion, toVersion);
            WalkerUtils.convert(MCTypeRegistry.DATA_COMPONENTS, root, "components", fromVersion, toVersion);

            return null;
        });

        // Custom converter for converting commands inside signs, books, command blocks
        V3818_Commands.register_5();

        // Step 6
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:area_effect_cloud", new DataConverter<>(VERSION, 6) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final Object color = data.getGeneric("Color");
                final Object effects = data.getGeneric("effects");
                final Object potion = data.getGeneric("Potion");

                if (color == null && effects == null && potion == null) {
                    return null;
                }
                data.remove("Color");
                data.remove("effects");
                data.remove("Potion");

                final MapType<String> potionContents = data.getTypeUtil().createEmptyMap();
                data.setMap("potion_contents", potionContents);

                if (color != null) {
                    potionContents.setGeneric("custom_color", color);
                }

                if (effects != null) {
                    potionContents.setGeneric("custom_effects", effects);
                }

                if (potion != null) {
                    potionContents.setGeneric("potion", potion);
                }

                return null;
            }
        });
    }

    private V3818() {}
}
