package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4763 {

    private static final int VERSION = MCVersions.V1_21_11 + 92;

    public static void register() {
        final DataConverter<MapType, MapType> converter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                data.setBoolean("VillagerDataFinalized", true);
                return null;
            }
        };

        MCTypeRegistry.ENTITY.addConverterForId("minecraft:villager", converter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:zombie_villager", converter);
    }

    private V4763() {}
}
