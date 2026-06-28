package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.converters.datatypes.DataWalker;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import ca.spottedleaf.dataconverter.types.TypeUtil;

public final class V4059 {

    private static final int VERSION = MCVersions.V24W33A + 1;

    public static void register() {
        // previous version: 3818.3
        // next version: 4307
        MCTypeRegistry.DATA_COMPONENTS.addStructureWalker(VERSION, new DataWalker<>() {
            private static void walkBlockPredicates(final MapType root, final long fromVersion, final long toVersion) {
                if (root.hasKey("blocks", ObjectType.STRING)) {
                    WalkerUtils.convert(MCTypeRegistry.BLOCK_NAME, root, "blocks", fromVersion, toVersion);
                } else if (root.hasKey("blocks", ObjectType.LIST)) {
                    WalkerUtils.convertList(MCTypeRegistry.BLOCK_NAME, root, "blocks", fromVersion, toVersion);
                }
            }

            @Override
            public MapType walk(final MapType root, final long fromVersion, final long toVersion) {
                WalkerUtils.convertListPath(MCTypeRegistry.ENTITY, root, "minecraft:bees", "entity_data", fromVersion, toVersion);

                WalkerUtils.convert(MCTypeRegistry.TILE_ENTITY, root, "minecraft:block_entity_data", fromVersion, toVersion);
                WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, root, "minecraft:bundle_contents", fromVersion, toVersion);

                final MapType canBreak = root.getMap("minecraft:can_break");
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

                final MapType canPlaceOn = root.getMap("minecraft:can_place_on");
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
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, root, "minecraft:use_remainder", fromVersion, toVersion);

                final MapType equippable = root.getMap("minecraft:equippable");
                if (equippable != null) {
                    WalkerUtils.convert(MCTypeRegistry.ENTITY_NAME, equippable, "allowed_entities", fromVersion, toVersion);
                    WalkerUtils.convertList(MCTypeRegistry.ENTITY_NAME, equippable, "allowed_entities", fromVersion, toVersion);
                }

                WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, root, "minecraft:custom_name", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, root, "minecraft:item_name", fromVersion, toVersion);
                WalkerUtils.convertList(MCTypeRegistry.TEXT_COMPONENT, root, "minecraft:lore", fromVersion, toVersion);

                final MapType writtenBookContent = root.getMap("minecraft:written_book_content");
                if (writtenBookContent != null) {
                    final ListType pages = writtenBookContent.getListUnchecked("pages");
                    // This logic needs to correctly handle both the NBT format of TEXT_COMPONENT and the JSON format.
                    if (pages != null) {
                        for (int i = 0, len = pages.size(); i < len; ++i) {
                            final Object pageGeneric = pages.getGeneric(i);
                            final boolean isNBTFormat = fromVersion >= DataConverter.encodeVersions(V4290.VERSION, 0);
                            // Note: We only parse ListType for 4290 and above as only a String was valid JSON. List of anything was not valid.
                            if (pageGeneric instanceof String || (isNBTFormat && pageGeneric instanceof ListType)) { // handles: String case (JSON/NBT), ListType case (NBT)
                                final Object convertedGeneric = MCTypeRegistry.TEXT_COMPONENT.convert(pageGeneric, fromVersion, toVersion);
                                if (convertedGeneric != null) {
                                    pages.setGeneric(i, convertedGeneric);
                                }
                            } else if (pageGeneric instanceof MapType mapType) {
                                // Need to handle: Filterable format and regular NBT Component format are both MapType...
                                if (mapType.hasKey("raw") || mapType.hasKey("filtered")) {
                                    // Assume filterable format
                                    WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, mapType, "raw", fromVersion, toVersion);
                                    WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, mapType, "filtered", fromVersion, toVersion);
                                } else if (isNBTFormat) {
                                    final Object convertedGeneric = MCTypeRegistry.TEXT_COMPONENT.convert(mapType, fromVersion, toVersion);
                                    if (convertedGeneric != null) {
                                        pages.setGeneric(i, convertedGeneric);
                                    }
                                } // else: invalid data
                            } // else: invalid data
                        }
                    }
                }

                return null;
            }
        });

        MCTypeRegistry.DATA_COMPONENTS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType food = data.getMap("minecraft:food");
                if (food == null) {
                    return null;
                }

                final TypeUtil<?> typeUtil = data.getTypeUtil();

                final float eatSeconds = food.getFloat("eat_seconds", 1.6F);

                final ListType oldEffects = food.getList("effects", ObjectType.MAP);
                final ListType newEffects = typeUtil.createEmptyList();
                if (oldEffects != null) {
                    for (int i = 0, len = oldEffects.size(); i < len; ++i) {
                        final MapType oldEffect = oldEffects.getMap(i);

                        final MapType newEffect = typeUtil.createEmptyMap();
                        newEffects.addMap(newEffect);

                        newEffect.setString("type", "minecraft:apply_effects");

                        final Object oldEffectEffect = oldEffect.getGeneric("effect");
                        final ListType newEffectEffects = typeUtil.createEmptyList();
                        newEffectEffects.addGeneric(oldEffectEffect);
                        newEffect.setList("effects", newEffectEffects);

                        newEffect.setFloat("probability", oldEffect.getFloat("probability", 1.0F));
                    }
                }

                final Object convertsTo = food.getGeneric("using_converts_to");
                if (convertsTo != null) {
                    data.setGeneric("minecraft:use_remainder", convertsTo);
                }

                food.remove("eat_seconds");
                food.remove("effects");
                food.remove("using_converts_to");

                final MapType consumable = typeUtil.createEmptyMap();
                data.setMap("minecraft:consumable", consumable);

                consumable.setFloat("consume_seconds", eatSeconds);
                consumable.setList("on_consume_effects", newEffects);

                return null;
            }
        });
    }

    private V4059() {}
}
