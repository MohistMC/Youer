package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V2537 {

    private static final int VERSION = MCVersions.V20W20B;

    private static void convertDimension(final MapType data, final String path) {
        if (data == null) {
            return;
        }

        final Number dimension = data.getNumber(path);
        if (dimension == null) {
            return;
        }

        final String newDimension;
        switch (dimension.intValue()) {
            case -1: {
                newDimension = "minecraft:the_nether";
                break;
            }
            case 1: {
                newDimension = "minecraft:the_end";
                break;
            }
            default: {
                newDimension = "minecraft:overworld";
                break;
            }
        }

        data.setString(path, newDimension);
        return;
    }

    public static void register() {
        MCTypeRegistry.PLAYER.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                convertDimension(data, "Dimension");
                return null;
            }
        });

        MCTypeRegistry.SAVED_DATA_MAP_DATA.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType root, final long sourceVersion, final long toVersion) {
                convertDimension(root.getMap("data"), "dimension");
                return null;
            }
        });
    }

    private V2537() {}
}
