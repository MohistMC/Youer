package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V2538 {

    private static final int VERSION = MCVersions.V20W20B + 1;
    private static final String[] MERGE_KEYS = new String[] {
            "RandomSeed",
            "generatorName",
            "generatorOptions",
            "generatorVersion",
            "legacy_custom_options",
            "MapFeatures",
            "BonusChest"
    };

    public static void register() {
        MCTypeRegistry.LEVEL.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> worldGenSettings = data.getOrCreateMap("WorldGenSettings");

                for (final String key : MERGE_KEYS) {
                    final Object value = data.getGeneric(key);
                    if (value == null) {
                        continue;
                    }

                    data.remove(key);
                    worldGenSettings.setGeneric(key, value);
                }

                return null;
            }
        });
    }

    private V2538() {}
}
