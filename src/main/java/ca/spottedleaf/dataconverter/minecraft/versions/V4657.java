package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4657 {

    private static final int VERSION = MCVersions.V25W43A + 2;

    public static void register() {
        MCTypeRegistry.SAVED_DATA_WORLD_BORDER.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType root, final long sourceVersion, final long toVersion) {
                final MapType data = root.getMap("data");
                if (data == null) {
                    return null;
                }

                data.setInt("warning_time", data.getInt("warning_time", 15) * 20);

                return null;
            }
        });
    }

    private V4657() {}
}
