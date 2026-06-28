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
import java.util.LinkedHashSet;
import java.util.Set;

public final class V4307 {

    private static final int VERSION = MCVersions.V25W03A + 3;

    public static void register() {
        MCTypeRegistry.DATA_COMPONENTS.addStructureConverter(new DataConverter<>(VERSION) {
            private static final String[] ADDITIONAL_TOOLTIP_COMPONENTS = new String[] {
                "minecraft:banner_patterns",
                "minecraft:bees",
                "minecraft:block_entity_data",
                "minecraft:block_state",
                "minecraft:bundle_contents",
                "minecraft:charged_projectiles",
                "minecraft:container",
                "minecraft:container_loot",
                "minecraft:firework_explosion",
                "minecraft:fireworks",
                "minecraft:instrument",
                "minecraft:map_id",
                "minecraft:painting/variant",
                "minecraft:pot_decorations",
                "minecraft:potion_contents",
                "minecraft:tropical_fish/pattern",
                "minecraft:written_book_content"
            };

            private static void unwrapBlockPredicates(final MapType root, final String path, final Set<String> hiddenComponents) {
                final MapType component = root.getMap(path);
                if (component == null) {
                    return;
                }

                final Object predicates = component.getGeneric("predicates");
                if (predicates == null) {
                    return;
                }
                root.setGeneric(path, predicates);
                if (!component.getBoolean("show_in_tooltip", true)) {
                    hiddenComponents.add(path);
                }
            }

            private static void updateComponent(final MapType root, final String path, final Set<String> hiddenComponents) {
                final MapType component = root.getMap(path);
                if (component == null) {
                    return;
                }

                if (!component.getBoolean("show_in_tooltip", true)) {
                    hiddenComponents.add(path);
                }

                component.remove("show_in_tooltip");
            }

            private static void updateComponentAndUnwrap(final MapType root, final String componentPath, final String unwrapPath, final Set<String> hiddenComponents) {
                final MapType component = root.getMap(componentPath);
                if (component == null) {
                    return;
                }

                if (!component.getBoolean("show_in_tooltip", true)) {
                    hiddenComponents.add(componentPath);
                }

                component.remove("show_in_tooltip");

                final Object wrapped = component.getGeneric(unwrapPath);
                if (wrapped != null) {
                    root.setGeneric(componentPath, wrapped);
                }
            }

            @Override
            public MapType convert(final MapType root, final long sourceVersion, final long toVersion) {
                // Don't use a linked hash set, and ensure that it is added in the same (undefined) order as the vanilla datafixer
                final Set<String> hiddenComponents = new java.util.HashSet<>();
                unwrapBlockPredicates(root, "minecraft:can_place_on", hiddenComponents);
                unwrapBlockPredicates(root, "minecraft:can_break", hiddenComponents);

                updateComponent(root, "minecraft:trim", hiddenComponents);
                updateComponent(root, "minecraft:unbreakable", hiddenComponents);

                updateComponentAndUnwrap(root, "minecraft:dyed_color", "rgb", hiddenComponents);
                updateComponentAndUnwrap(root, "minecraft:attribute_modifiers", "modifiers", hiddenComponents);
                updateComponentAndUnwrap(root, "minecraft:enchantments", "levels", hiddenComponents);
                updateComponentAndUnwrap(root, "minecraft:stored_enchantments", "levels", hiddenComponents);
                updateComponentAndUnwrap(root, "minecraft:jukebox_playable", "song", hiddenComponents);

                final boolean hideTooltip = root.hasKey("minecraft:hide_tooltip");
                final boolean hideAdditionalTooltip = root.hasKey("minecraft:hide_additional_tooltip");

                if (hideAdditionalTooltip) {
                    for (final String component : ADDITIONAL_TOOLTIP_COMPONENTS) {
                        if (root.hasKey(component)) {
                            hiddenComponents.add(component);
                        }
                    }
                }

                root.remove("minecraft:hide_tooltip");
                root.remove("minecraft:hide_additional_tooltip");

                if (hideTooltip || !hiddenComponents.isEmpty()) {
                    final TypeUtil<?> typeUtil = root.getTypeUtil();

                    final MapType tooltipDisplay = typeUtil.createEmptyMap();
                    final ListType hiddenComponentsList = typeUtil.createEmptyList();

                    root.setMap("minecraft:tooltip_display", tooltipDisplay);
                    tooltipDisplay.setBoolean("hide_tooltip", hideTooltip);
                    tooltipDisplay.setList("hidden_components", hiddenComponentsList);

                    for (final String component : hiddenComponents) {
                        hiddenComponentsList.addString(component);
                    }
                }

                return null;
            }
        });

        // previous version: 4059
        MCTypeRegistry.DATA_COMPONENTS.addStructureWalker(VERSION, new DataWalker<>() {
            private static void walkBlockPredicate(final MapType data, final long fromVersion, final long toVersion) {
                if (data == null) {
                    return;
                }

                if (data.hasKey("blocks", ObjectType.LIST)) {
                    WalkerUtils.convertList(MCTypeRegistry.BLOCK_NAME, data, "blocks", fromVersion, toVersion);
                } else {
                    WalkerUtils.convert(MCTypeRegistry.BLOCK_NAME, data, "blocks", fromVersion, toVersion);
                }
            }

            private static void walkBlockPredicates(final MapType root, final String path, final long fromVersion, final long toVersion) {
                final Object value = root.getGeneric(path);

                if (value instanceof MapType data) {
                    walkBlockPredicate(data, fromVersion, toVersion);
                } else if (value instanceof ListType list) {
                    for (int i = 0, len = list.size(); i < len; ++i) {
                        walkBlockPredicate(list.getMap(i, null), fromVersion, toVersion);
                    }
                }
            }

            @Override
            public MapType walk(final MapType root, final long fromVersion, final long toVersion) {
                WalkerUtils.convertListPath(MCTypeRegistry.ENTITY, root, "minecraft:bees", "entity_data", fromVersion, toVersion);

                WalkerUtils.convert(MCTypeRegistry.TILE_ENTITY, root, "minecraft:block_entity_data", fromVersion, toVersion);
                WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, root, "minecraft:bundle_contents", fromVersion, toVersion);

                walkBlockPredicates(root, "minecraft:can_break", fromVersion, toVersion);
                walkBlockPredicates(root, "minecraft:can_place_on", fromVersion, toVersion);

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
                    if (pages != null) {
                        for (int i = 0, len = pages.size(); i < len; ++i) {
                            final Object pageGeneric = pages.getGeneric(i);
                            if (pageGeneric instanceof String || pageGeneric instanceof ListType) { // handles: String case, ListType case
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
                                } else {
                                    // Assume regular NBT format
                                    final Object convertedGeneric = MCTypeRegistry.TEXT_COMPONENT.convert(pageGeneric, fromVersion, toVersion);
                                    if (convertedGeneric != null) {
                                        pages.setGeneric(i, convertedGeneric);
                                    }
                                }
                            }
                        }
                    }
                }

                return null;
            }
        });
    }

    private V4307() {}
}
