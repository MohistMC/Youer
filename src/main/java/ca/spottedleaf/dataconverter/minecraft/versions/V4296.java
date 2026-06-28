package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4296 {

    private static final int VERSION = MCVersions.V1_21_4 + 107;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:area_effect_cloud", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                data.setFloat("potion_duration_scale", 0.25f);
                return null;
            }
        });
    }

    private V4296() {}
}
