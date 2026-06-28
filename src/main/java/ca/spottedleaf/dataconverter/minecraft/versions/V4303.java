package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4303 {

    private static final int VERSION = MCVersions.V25W02A + 5;

    public static void register() {
        final DataConverter<MapType, MapType> fallConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (!data.hasKey("FallDistance")) {
                    return null;
                }
                final float fallDistance = data.getFloat("FallDistance", 0.0f);
                data.remove("FallDistance");

                data.setDouble("fall_distance", (double)fallDistance);

                return null;
            }
        };

        MCTypeRegistry.ENTITY.addStructureConverter(fallConverter);
        MCTypeRegistry.PLAYER.addStructureConverter(fallConverter);
    }

    private V4303() {}
}
