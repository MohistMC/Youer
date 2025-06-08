package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V1500 {

    private static final int VERSION = MCVersions.V18W22C + 1;

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addConverterForId("DUMMY", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                data.setBoolean("keepPacked", true);
                return null;
            }
        });
    }

    private V1500() {}
}
