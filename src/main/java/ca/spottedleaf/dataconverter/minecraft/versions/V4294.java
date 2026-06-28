package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4294 {

    private static final int VERSION = MCVersions.V1_21_4 + 105;

    public static void register() {
        MCTypeRegistry.BLOCK_STATE.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (!"minecraft:creaking_heart".equals(data.getString("Name"))) {
                    return null;
                }

                final MapType properties = data.getMap("Properties");
                if (properties == null) {
                    return null;
                }

                final String active = properties.getString("active");
                if (active == null) {
                    return null;
                }
                properties.remove("active");
                properties.setString("creaking_heart_state", active.equals("true") ? "awake" : "uprooted");

                return null;
            }
        });
    }

    private V4294() {}
}
