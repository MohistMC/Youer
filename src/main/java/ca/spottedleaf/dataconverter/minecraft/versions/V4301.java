package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4301 {

    private static final int VERSION = MCVersions.V25W02A + 3;

    public static void register() {
        MCTypeRegistry.ENTITY_EQUIPMENT.addStructureConverter(new DataConverter<>(VERSION) {
            private static final String[] ARMOR_SLOTS = new String[] {
                "feet",
                "legs",
                "chest",
                "head"
            };

            private static final String[] HAND_SLOTS = new String[] {
                "mainhand",
                "offhand"
            };

            private static MapType filterItem(final MapType item) {
                return item == null || item.hasKey("id") ? item : null;
            }

            private static void setIfNonNull(final MapType dst, final String dstKey, final MapType value) {
                if (value != null) {
                    dst.setMap(dstKey, value);
                }
            }

            private static void moveItems(final MapType src, final String srcPath, final String[] names, final MapType dst) {
                final ListType items = src.getListUnchecked(srcPath);
                if (items == null) {
                    return;
                }

                for (int i = 0, len = Math.min(items.size(), names.length); i < len; ++i) {
                    final MapType item = filterItem(items.getMap(i, null));
                    if (item != null) {
                        dst.setMap(names[i], item);
                    }
                }
            }

            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType equipment = data.getTypeUtil().createEmptyMap();

                setIfNonNull(equipment, "body", filterItem(data.getMap("body_armor_item")));
                setIfNonNull(equipment, "saddle", filterItem(data.getMap("saddle")));

                moveItems(data, "ArmorItems", ARMOR_SLOTS, equipment);
                moveItems(data, "HandItems", HAND_SLOTS, equipment);

                data.remove("ArmorItems");
                data.remove("HandItems");
                data.remove("body_armor_item");
                data.remove("saddle");

                if (!equipment.isEmpty()) {
                    data.setMap("equipment", equipment);
                }

                return null;
            }
        });

        MCTypeRegistry.ENTITY_EQUIPMENT.addStructureWalker(VERSION, (final MapType data, final long fromVersion, final long toVersion) -> {
            final MapType equipment = data.getMap("equipment");
            if (equipment != null) {
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, equipment, "mainhand", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, equipment, "offhand", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, equipment, "feet", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, equipment, "legs", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, equipment, "chest", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, equipment, "head", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, equipment, "body", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, equipment, "saddle", fromVersion, toVersion);
            }

            return null;
        });
    }

    private V4301() {}
}
