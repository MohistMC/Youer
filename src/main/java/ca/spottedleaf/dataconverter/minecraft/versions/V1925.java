package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.Types;

public final class V1925 {

    private static final int VERSION = MCVersions.V19W03C + 1;

    public static void register() {
        MCTypeRegistry.SAVED_DATA_MAP_DATA.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType root, final long sourceVersion, final long toVersion) {
                final MapType data = root.getMap("data");
                if (data == null) {
                    final MapType ret = Types.NBT.createEmptyMap();
                    ret.setMap("data", root);

                    return ret;
                }
                return null;
            }
        });
        MCTypeRegistry.SAVED_DATA_MAP_DATA.addStructureWalker(VERSION, (final MapType root, final long fromVersion, final long toVersion) -> {
            WalkerUtils.convertListPath(MCTypeRegistry.TEXT_COMPONENT, root.getMap("data"), "banners", "Name", fromVersion, toVersion);
            return null;
        });
    }

    private V1925() {}
}
