package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;

public final class V4312 {

    private static final int VERSION = MCVersions.V25W05A + 2;

    public static void register() {
        MCTypeRegistry.PLAYER.addStructureConverter(new DataConverter<>(VERSION) {
            private static final Int2ObjectOpenHashMap<String> SLOT_MOVE_MAPPING = new Int2ObjectOpenHashMap<>(
                Map.of(
                    100, "feet",
                    101, "legs",
                    102, "chest",
                    103, "head",
                    -106, "offhand"
                )
            );

            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final ListType inventory = data.getListUnchecked("Inventory");
                if (inventory == null) {
                    return null;
                }

                final MapType equipment = data.getTypeUtil().createEmptyMap();

                for (int i = 0; i < inventory.size(); ++i) {
                    final MapType item = inventory.getMap(i, null);
                    if (item == null) {
                        continue;
                    }

                    final int slot = item.getInt("Slot", Integer.MIN_VALUE);
                    final String equipmentKey = SLOT_MOVE_MAPPING.get(slot);

                    if (equipmentKey == null) {
                        continue;
                    }

                    inventory.remove(i--);

                    item.remove("Slot");
                    equipment.setMap(equipmentKey, item);
                }

                data.setMap("equipment", equipment);

                return null;
            }
        });

        // Note: See V1458 for why we walk CustomName
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

            return MCTypeRegistry.ENTITY_EQUIPMENT.convert(data, fromVersion, toVersion);
        });

    }

    private V4312() {}
}
