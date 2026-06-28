package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4305 {

    private static final int VERSION = MCVersions.V25W03A + 1;

    public static void register() {
        MCTypeRegistry.BLOCK_STATE.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (!"minecraft:test_block".equals(data.getString("Name"))) {
                    return null;
                }

                final MapType properties = data.getMap("Properties");
                if (properties == null) {
                    return null;
                }

                final String mode = properties.getString("test_block_mode");
                if (mode == null) {
                    return null;
                }
                properties.remove("test_block_mode");
                properties.setString("mode", mode);

                return null;
            }
        });
    }

    private V4305() {}
}
