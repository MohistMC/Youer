package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.datatypes.DataWalker;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class V4996 {

    private static final int VERSION = MCVersions.V26_2 + 93;

    private static final String[] SIDES = {"back", "left", "right", "front"};

    private static void unpackDecorations(final MapType data, final String path, final boolean createMissing) {
        final Object old = data.getGeneric(path);
        if (!(old instanceof ListType) && !(old == null && createMissing)) {
            return;
        }

        final ListType list = old instanceof ListType decorations ? decorations : data.createEmptyList();
        final MapType unpacked = data.createEmptyMap();
        for (int i = 0; i < SIDES.length; ++i) {
            final String id = i < list.size() ? list.getString(i, "minecraft:brick") : "minecraft:brick";
            if (id.isEmpty()) {
                return;
            }
            final MapType item = data.createEmptyMap();
            item.setString("id", id);
            unpacked.setMap(SIDES[i], item);
        }
        data.setMap(path, unpacked);
    }

    private static void walkDecorations(final @Nullable MapType decorations, final long fromVersion, final long toVersion) {
        if (decorations == null) {
            return;
        }
        for (final String side : SIDES) {
            WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, decorations, side, fromVersion, toVersion);
        }
    }

    public static void register() {
        MCTypeRegistry.DATA_COMPONENTS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                unpackDecorations(data, "minecraft:pot_decorations", false);
                return null;
            }
        });
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:decorated_pot", new DataConverter<>(VERSION, 1) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                unpackDecorations(data, "sherds", true);
                return null;
            }
        });
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, 1, "minecraft:decorated_pot", new DataWalkerItems("item"));
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, 1, "minecraft:decorated_pot", new DataWalker<>() {
            @Override
            public @Nullable MapType walk(final MapType data, final long fromVersion, final long toVersion) {
                walkDecorations(data.getMap("sherds"), fromVersion, toVersion);
                return null;
            }
        });

        // previous version: 4307
        MCTypeRegistry.DATA_COMPONENTS.addStructureWalker(VERSION, new DataWalker<>() {
            private static void walkBlockPredicate(final @Nullable MapType data, final long fromVersion, final long toVersion) {
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
            public @Nullable MapType walk(final MapType root, final long fromVersion, final long toVersion) {
                WalkerUtils.convertListPath(MCTypeRegistry.ENTITY, root, "minecraft:bees", "entity_data", fromVersion, toVersion);

                WalkerUtils.convert(MCTypeRegistry.TILE_ENTITY, root, "minecraft:block_entity_data", fromVersion, toVersion);
                WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, root, "minecraft:bundle_contents", fromVersion, toVersion);

                walkBlockPredicates(root, "minecraft:can_break", fromVersion, toVersion);
                walkBlockPredicates(root, "minecraft:can_place_on", fromVersion, toVersion);

                WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, root, "minecraft:charged_projectiles", fromVersion, toVersion);
                WalkerUtils.convertListPath(MCTypeRegistry.ITEM_STACK, root, "minecraft:container", "item", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ENTITY, root, "minecraft:entity_data", fromVersion, toVersion);
                walkDecorations(root.getMap("minecraft:pot_decorations"), fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, root, "minecraft:sulfur_cube_content", fromVersion, toVersion);
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

    private V4996() {}
}
