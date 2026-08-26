package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V2679 {

    private static final int VERSION = MCVersions.V1_16_5 + 93;

    public static void register() {
        MCTypeRegistry.BLOCK_STATE.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (!"minecraft:cauldron".equals(data.getString("Name"))) {
                    return null;
                }

                final MapType properties = data.getMap("Properties");

                if (properties == null) {
                    return null;
                }

                if (properties.getString("level", "0").equals("0")) {
                    data.remove("Properties");
                    return null;
                } else {
                    data.setString("Name", "minecraft:water_cauldron");
                    return null;
                }
            }
        });
    }

    private V2679() {}
}
