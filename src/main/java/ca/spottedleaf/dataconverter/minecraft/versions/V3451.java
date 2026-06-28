package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;

public final class V3451 {

    private static final int VERSION = MCVersions.V23W16A + 2;

    public static void removeLight(final MapType data) {
        data.remove("isLightOn");

        final ListType sections = data.getList("sections", ObjectType.MAP);
        if (sections == null) {
            return;
        }

        for (int i = 0, len = sections.size(); i < len; ++i) {
            final MapType section = sections.getMap(i);

            section.remove("BlockLight");
            section.remove("SkyLight");
        }
    }

    public static void register() {
        MCTypeRegistry.CHUNK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                removeLight(data);

                return null;
            }
        });
    }

    private V3451() {}
}
