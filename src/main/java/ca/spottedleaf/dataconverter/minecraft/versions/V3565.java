package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V3565 {

    private static final int VERSION = MCVersions.V1_20_1 + 100;

    public static void register() {
        MCTypeRegistry.SAVED_DATA_RANDOM_SEQUENCES.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType root, final long sourceVersion, final long toVersion) {
                final MapType oldData = root.getMap("data");
                if (oldData == null) {
                    return null;
                }

                final MapType newData = root.createEmptyMap();
                root.setMap("data", newData);

                newData.setMap("sequences", oldData);

                return null;
            }
        });
    }

    private V3565() {}
}
