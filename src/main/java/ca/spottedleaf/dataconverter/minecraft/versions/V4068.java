package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.TypeUtil;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

public final class V4068 {
    public static final Escaper ESCAPER = Escapers.builder().addEscape('"', "\\\"").addEscape('\\', "\\\\").build();

    private static final int VERSION = MCVersions.V24W38A + 2;

    private static void convertLock(final MapType root, final String srcPath, final String dstPath) {
        if (root == null) {
            return;
        }

        final Object lockGeneric = root.getGeneric(srcPath);
        if (lockGeneric == null) {
            return;
        }

        final TypeUtil<?> typeUtil = root.getTypeUtil();

        root.remove(srcPath);

        if (lockGeneric instanceof String lock && !lock.isEmpty()) {
            final MapType newLock = typeUtil.createEmptyMap();
            root.setMap(dstPath, newLock);

            final MapType lockComponents = typeUtil.createEmptyMap();
            newLock.setMap("components", lockComponents);

            lockComponents.setString("minecraft:custom_name", ESCAPER.escape(lock));
        }
    }

    public static void register() {
        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType components = data.getMap("components");
                if (components == null) {
                    return null;
                }

                convertLock(components, "minecraft:lock", "minecraft:lock");

                return null;
            }
        });
        MCTypeRegistry.TILE_ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                convertLock(data, "Lock", "lock");
                return null;
            }
        });
    }

    private V4068() {}
}
