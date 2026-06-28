package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4057 {

    private static final int VERSION = MCVersions.V1_21_1 + 102;

    public static void register() {
        MCTypeRegistry.CHUNK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType carvingMasks = data.getMap("CarvingMasks");
                if (carvingMasks == null) {
                    return null;
                }
                data.remove("CarvingMasks");

                final long[] airMask = carvingMasks.getLongs("AIR");
                if (airMask != null) {
                    data.setLongs("carving_mask", airMask);
                }

                return null;
            }
        });
    }

    private V4057() {}
}
