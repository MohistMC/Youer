package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4439 {

    private static final int VERSION = MCVersions.V1_21_8_RC1;

    public static void register() {
        MCTypeRegistry.ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (data.getDouble("fall_distance", -1.0) == 0.0) {
                    data.remove("fall_distance");
                }
                return null;
            }
        });
    }

    private V4439() {}
}
