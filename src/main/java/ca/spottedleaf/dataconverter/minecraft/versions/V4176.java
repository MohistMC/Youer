package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4176 {

    private static final int VERSION = MCVersions.V24W44A + 2;

    private static void fixInvalidLock(final MapType root, final String path) {
        final MapType lock = root.getMap(path);
        if (lock == null || lock.size() != 1) {
            return;
        }

        final MapType components = lock.getMap("components");
        if (components == null || components.size() != 1 || !"\"\"".equals(components.getString("minecraft:custom_name"))) {
            return;
        }

        root.remove(path);
    }

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                fixInvalidLock(data, "lock");
                return null;
            }
        });
        MCTypeRegistry.DATA_COMPONENTS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                fixInvalidLock(data, "minecraft:lock");
                return null;
            }
        });
    }

    private V4176() {}
}
