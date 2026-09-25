package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;

public final class V2211 {

    private static final int VERSION = MCVersions.V19W41A + 1;

    public static void register() {
        MCTypeRegistry.STRUCTURE_FEATURE.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (!data.hasKey("references", ObjectType.NUMBER)) {
                    return null;
                }

                final int references = data.getInt("references");
                if (references <= 0) {
                    data.setInt("references", 1);
                }
                return null;
            }
        });
    }

    private V2211() {}
}
