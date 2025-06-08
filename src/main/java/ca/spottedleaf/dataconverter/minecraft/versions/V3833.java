package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;

public final class V3833 {

    private static final int VERSION = MCVersions.V1_20_5_PRE4 + 1;

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:brushable_block", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> item = data.getMap("item");
                if (item == null) {
                    return null;
                }


                final String id = NamespaceUtil.correctNamespace(item.getString("id", "minecraft:air"));
                final int count = item.getInt("count", 0);

                // Fix DFU: use count <= 0 instead of count == 0
                if ("minecraft:air".equals(id) || count <= 0) {
                    data.remove("item");
                }

                return null;
            }
        });
    }

    private V3833() {}
}
