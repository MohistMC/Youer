package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V3459 {

    private static final int VERSION = MCVersions.V1_20_PRE5 + 1;

    public static void register() {
        MCTypeRegistry.LEVEL.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (data.hasKey("DragonFight")) {
                    return null;
                }

                final MapType dimensionData = data.getMap("DimensionData");
                if (dimensionData == null) {
                    return null;
                }

                final MapType endData = dimensionData.getMap("1");
                if (endData != null) {
                    final MapType dragonFight = endData.getMap("DragonFight", endData.createEmptyMap()).copy();
                    V3807.flattenBlockPos(dragonFight, "ExitPortalLocation");
                    data.setMap("DragonFight", dragonFight);
                }

                return null;
            }
        });
    }

    private V3459() {}
}
