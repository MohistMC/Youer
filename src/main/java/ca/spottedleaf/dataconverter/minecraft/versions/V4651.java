package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4651 {

    public static final int VERSION = MCVersions.V1_21_10 + 95;

    public static void register() {
        MCTypeRegistry.OPTIONS.addStructureConverter(new DataConverter<>(VERSION) {
            private static void convertTo(final String graphicsMode, final MapType dst, final String dstPath,
                                          final String fast, final String fancy, final String fabulous) {
                final String toSet;
                if (graphicsMode == null) {
                    toSet = fancy;
                } else {
                    switch (graphicsMode) {
                        case "0": {
                            toSet = fast;
                            break;
                        }
                        default: {
                            toSet = fancy;
                            break;
                        }
                        case "2": {
                            toSet = fabulous;
                            break;
                        }
                    }
                }

                dst.setString(dstPath, toSet);
            }

            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final String graphicsMode = data.getString("graphicsMode");

                convertTo(graphicsMode, data, "cutoutLeaves", "false", "true", "true");
                convertTo(graphicsMode, data, "weatherRadius", "5", "10", "10");
                convertTo(graphicsMode, data, "vignette", "false", "true", "true");
                convertTo(graphicsMode, data, "improvedTransparency", "false", "false", "true");

                return null;
            }
        });
        MCTypeRegistry.OPTIONS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                data.setString("graphicsPreset", "custom");
                return null;
            }
        });
    }

    private V4651() {}
}
