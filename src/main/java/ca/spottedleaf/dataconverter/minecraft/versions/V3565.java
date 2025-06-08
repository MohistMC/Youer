package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V3565 {

    private static final int VERSION = MCVersions.V1_20_1 + 100;

    public static void register() {
        MCTypeRegistry.SAVED_DATA_RANDOM_SEQUENCES.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> root, final long sourceVersion, final long toVersion) {
                final MapType<String> oldData = root.getMap("data");
                if (oldData == null) {
                    return null;
                }

                final MapType<String> newData = root.getTypeUtil().createEmptyMap();
                root.setMap("data", newData);

                newData.setMap("sequences", oldData);

                return null;
            }
        });
    }

    private V3565() {}
}
