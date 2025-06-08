package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.util.ComponentUtils;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V101 {

    private static final int VERSION = MCVersions.V15W32A + 1;

    private static void updateLine(final MapType<String> data, final String path) {
        final String textString = data.getString(path);

        if (textString == null) {
            return;
        }

        data.setString(path, ComponentUtils.convertFromLenient(textString));
    }

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addConverterForId("Sign", new DataConverter<>(VERSION) {

            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                updateLine(data, "Text1");
                updateLine(data, "Text2");
                updateLine(data, "Text3");
                updateLine(data, "Text4");
                return null;
            }
        });
    }

    private V101() {}
}
