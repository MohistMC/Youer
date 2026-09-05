package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.converter.types.MapType;

public final class V147 {

    private static final int VERSION = MCVersions.V15W46A + 1;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("ArmorStand", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (data.getBoolean("Silent") && !data.getBoolean("Marker")) {
                    data.remove("Silent");
                }

                return null;
            }
        });
    }

    private V147() {}
}
