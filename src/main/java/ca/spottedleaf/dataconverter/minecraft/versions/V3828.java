package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;

public final class V3828 {

    private static final int VERSION = MCVersions.V24W14A + 1;

    public static void register() {
        MCTypeRegistry.VILLAGER_TRADE.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> buyB = data.getMap("buyB");

                if (buyB == null) {
                    return null;
                }

                final String id = NamespaceUtil.correctNamespace(buyB.getString("id", "minecraft:air"));
                final int count = buyB.getInt("count", 0);

                // Fix DFU: use count <= 0 instead of count == 0
                if ("minecraft:air".equals(id) || count <= 0) {
                    data.remove("buyB");
                }

                return null;
            }
        });
    }

    private V3828() {}
}
