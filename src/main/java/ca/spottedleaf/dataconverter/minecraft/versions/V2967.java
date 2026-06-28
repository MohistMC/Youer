package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V2967 {

    private static final int VERSION = MCVersions.V22W05A;

    public static void register() {
        MCTypeRegistry.WORLD_GEN_SETTINGS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType dimensions = data.getMap("dimensions");

                if (dimensions == null) {
                    return null;
                }

                for (final String dimension : dimensions.keys()) {
                    final MapType dimensionData = dimensions.getMap(dimension);
                    if (dimensionData == null) {
                        continue;
                    }

                    final MapType generator = dimensionData.getMap("generator");
                    if (generator == null) {
                        continue;
                    }

                    final MapType settings = generator.getMap("settings");
                    if (settings == null) {
                        continue;
                    }

                    final MapType structures = settings.getMap("structures");
                    if (structures == null) {
                        continue;
                    }

                    for (final String structureKey : structures.keys()) {
                        structures.getMap(structureKey).setString("type", "minecraft:random_spread");
                    }

                    final MapType stronghold = structures.getMap("stronghold");
                    stronghold.setString("type", "minecraft:concentric_rings");
                    structures.setMap("minecraft:stronghold", stronghold.copy());
                }

                return null;
            }
        });
    }

    private V2967() {}
}
