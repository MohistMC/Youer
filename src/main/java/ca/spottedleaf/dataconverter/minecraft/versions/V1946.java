package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.TypeUtil;

public final class V1946 {

    private static final int VERSION = MCVersions.V19W14B + 1;

    public static void register() {
        MCTypeRegistry.POI_CHUNK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final TypeUtil<?> typeUtil = data.getTypeUtil();
                final MapType sections = typeUtil.createEmptyMap();
                data.setMap("Sections", sections);

                for (int y = 0; y < 16; ++y) {
                    final String key = Integer.toString(y);
                    final Object records = data.getGeneric(key);

                    if (records == null) {
                        continue;
                    }

                    data.remove(key);

                    final MapType section = typeUtil.createEmptyMap();
                    section.setGeneric("Records", records);
                    sections.setMap(key, section);
                }

                return null;
            }
        });
    }

    private V1946() {}
}
