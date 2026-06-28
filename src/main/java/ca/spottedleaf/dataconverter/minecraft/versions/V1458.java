package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.util.ComponentUtils;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V1458 {

    public static final int VERSION = MCVersions.V17W50A + 1;

    public static MapType updateCustomName(final MapType data) {
        final String customName = data.getString("CustomName", "");

        if (customName.isEmpty()) {
            data.remove("CustomName");
        } else {
            data.setString("CustomName", ComponentUtils.createPlainTextComponent(customName));
        }

        return null;
    }

    static void named(final int version, final String id) {
        MCTypeRegistry.TILE_ENTITY.addWalker(version, id, new DataWalkerTypePaths<>(MCTypeRegistry.TEXT_COMPONENT, "CustomName"));
    }

    static void namedInventory(final int version, final String id) {
        named(version, id);
        MCTypeRegistry.TILE_ENTITY.addWalker(version, id, new DataWalkerItemLists("Items"));
    }

    public static void register() {
        // From CB
        MCTypeRegistry.PLAYER.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                return updateCustomName(data);
            }
        });

        MCTypeRegistry.ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if ("minecraft:commandblock_minecart".equals(data.getString("id"))) {
                    return null;
                }

                return updateCustomName(data);
            }
        });

        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType tag = data.getMap("tag");
                if (tag == null) {
                    return null;
                }

                final MapType display = tag.getMap("display");
                if (display == null) {
                    return null;
                }

                final String name = display.getString("Name");
                if (name != null) {
                    display.setString("Name", ComponentUtils.createPlainTextComponent(name));
                } /* In 1.20.5, Mojang removed this branch (ItemCustomNameToComponentFix) */ /*else {
                    final String localisedName = display.getString("LocName");
                    if (localisedName != null) {
                        display.setString("Name", ComponentUtils.createTranslatableComponent(localisedName));
                        display.remove("LocName");
                    }
                }*/

                return null;
            }
        });

        MCTypeRegistry.TILE_ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if ("minecraft:command_block".equals(data.getString("id"))) {
                    return null;
                }

                return updateCustomName(data);
            }
        });

        MCTypeRegistry.ENTITY.addStructureWalker(VERSION, (final MapType data, final long fromVersion, final long toVersion) -> {
            WalkerUtils.convertList(MCTypeRegistry.ENTITY, data, "Passengers", fromVersion, toVersion);
            WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, data, "CustomName", fromVersion, toVersion);

            return MCTypeRegistry.ENTITY_EQUIPMENT.convert(data, fromVersion, toVersion);
        });

        // Note: This is not present in Vanilla, but since we have the converter from CB it makes sense that we
        //       would walk the CustomName field for Player if we are converting it
        MCTypeRegistry.PLAYER.addStructureWalker(VERSION, (final MapType data, final long fromVersion, final long toVersion) -> {
            WalkerUtils.convert(MCTypeRegistry.ENTITY, data.getMap("RootVehicle"), "Entity", fromVersion, toVersion);

            WalkerUtils.convertList(MCTypeRegistry.ENTITY, data, "ender_pearls", fromVersion, toVersion);

            WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, data, "Inventory", fromVersion, toVersion);
            WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, data, "EnderItems", fromVersion, toVersion);

            WalkerUtils.convert(MCTypeRegistry.ENTITY, data, "ShoulderEntityLeft", fromVersion, toVersion);
            WalkerUtils.convert(MCTypeRegistry.ENTITY, data, "ShoulderEntityRight", fromVersion, toVersion);

            final MapType recipeBook = data.getMap("recipeBook");
            if (recipeBook != null) {
                WalkerUtils.convertList(MCTypeRegistry.RECIPE, recipeBook, "recipes", fromVersion, toVersion);
                WalkerUtils.convertList(MCTypeRegistry.RECIPE, recipeBook, "toBeDisplayed", fromVersion, toVersion);
            }

            // "From CB"
            WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, data, "CustomName", fromVersion, toVersion);

            return null;
        });

        named(VERSION, "minecraft:beacon");
        named(VERSION, "minecraft:banner");
        namedInventory(VERSION, "minecraft:brewing_stand");
        namedInventory(VERSION, "minecraft:chest");
        namedInventory(VERSION, "minecraft:trapped_chest");
        namedInventory(VERSION, "minecraft:dispenser");
        namedInventory(VERSION, "minecraft:dropper");
        named(VERSION, "minecraft:enchanting_table");
        namedInventory(VERSION, "minecraft:furnace");
        namedInventory(VERSION, "minecraft:hopper");
        namedInventory(VERSION, "minecraft:shulker_box");
    }

    private V1458() {}
}
