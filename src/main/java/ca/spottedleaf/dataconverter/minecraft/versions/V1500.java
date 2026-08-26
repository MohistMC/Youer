package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V1500 {

    private static final int VERSION = MCVersions.V18W22C + 1;

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addConverterForId("DUMMY", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                data.setBoolean("keepPacked", true);
                return null;
            }
        });
    }

    private V1500() {}
}
