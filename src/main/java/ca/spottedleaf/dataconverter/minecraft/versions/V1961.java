package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V1961 {

    private static final int VERSION = MCVersions.V1_14_2_PRE3 + 1;

    public static void register() {
        MCTypeRegistry.CHUNK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType level = data.getMap("Level");
                if (level == null) {
                    return null;
                }

                level.remove("isLightOn");

                return null;
            }
        });
    }

    private V1961() {}
}
