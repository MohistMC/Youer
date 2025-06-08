package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;

public final class V3807 {

    private static final int VERSION = MCVersions.V24W04A + 1;

    public static void flattenBlockPos(final MapType<String> data, final String path) {
        if (data == null) {
            return;
        }

        final MapType<String> pos = data.getMap(path);
        if (pos == null) {
            return;
        }

        final Number x = pos.getNumber("X");
        final Number y = pos.getNumber("Y");
        final Number z = pos.getNumber("Z");

        if (x == null || y == null || z == null) {
            return;
        }

        data.setInts(path, new int[] { x.intValue(), y.intValue(), z.intValue() });
    }

    public static void register() {
        // Step 0
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:vault", (final MapType<String> root, final long fromVersion, final long toVersion) -> {
            WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, root.getMap("config"), "key_item", fromVersion, toVersion);
            WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, root.getMap("server_data"), "items_to_eject", fromVersion, toVersion);
            WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, root.getMap("shared_data"), "display_item", fromVersion, toVersion);

            return null;
        });

        // Step 1
        MCTypeRegistry.SAVED_DATA_MAP_DATA.addStructureConverter(new DataConverter<>(VERSION, 1) {
            @Override
            public MapType<String> convert(final MapType<String> root, final long sourceVersion, final long toVersion) {
                final MapType<String> data = root.getMap("data");

                if (data == null) {
                    return null;
                }

                final ListType banners = data.getList("banners", ObjectType.MAP);

                if (banners == null) {
                    return null;
                }

                for (int i = 0, len = banners.size(); i < len; ++i) {
                    V3807.flattenBlockPos(banners.getMap(i), "Pos");
                }

                return null;
            }
        });
    }

    private V3807() {}
}
