package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4291 {

    public static final int VERSION = MCVersions.V1_21_4 + 102;

    public static void register() {
        // V4290 correctly handles legacy hover events, so we skip that converter.
        MCTypeRegistry.TEXT_COMPONENT.addStructureConverter(new DataConverter<>(VERSION) {
            private static final String[] BOOLEAN_PATHS_TO_CONVERT = new String[] {
                "bold",
                "italic",
                "underlined",
                "strikethrough",
                "obfuscated",
                // add extra interpret field
                "interpret",
            };

            private static void convertToBoolean(final MapType data, final String path) {
                final String value = data.getString(path);
                if (value != null) {
                    data.setBoolean(path, Boolean.parseBoolean(value));
                }
            }

            @Override
            public Object convert(final Object data, final long sourceVersion, final long toVersion) {
                if (data instanceof MapType root) {
                    for (final String path : BOOLEAN_PATHS_TO_CONVERT) {
                        convertToBoolean(root, path);
                    }
                } // else: list -> handled by walker, string -> no formatting to convert
                return null;
            }
        });
    }

    private V4291() {}
}
