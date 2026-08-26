package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V1917 {

    private static final int VERSION = MCVersions.V18W49A + 1;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:cat", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (data.getInt("CatType") == 9) {
                    data.setInt("CatType", 10);
                }
                return null;
            }
        });
    }

    private V1917() {}
}
