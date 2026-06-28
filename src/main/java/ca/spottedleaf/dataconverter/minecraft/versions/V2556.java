package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V2556 {

    public static final int VERSION = MCVersions.V1_16_PRE1;

    public static void register() {
        MCTypeRegistry.OPTIONS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final String val = data.getString("fancyGraphics");
                if (val == null) {
                    return null;
                }

                data.remove("fancyGraphics");
                data.setString("graphicsMode", "true".equals(val) ? "1" : "0");

                return null;
            }
        });
    }

    private V2556() {}
}
