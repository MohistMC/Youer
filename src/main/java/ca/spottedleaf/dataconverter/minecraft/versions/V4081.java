package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4081 {

    private static final int VERSION = MCVersions.V1_21_2 + 1;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:salmon", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if ("large".equals(data.getString("type"))) {
                    return null;
                }

                data.setString("type", "medium");
                return null;
            }
        });
    }

    private V4081() {}
}
