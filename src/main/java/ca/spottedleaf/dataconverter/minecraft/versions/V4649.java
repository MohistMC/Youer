package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V4649 {

    public static final int VERSION = MCVersions.V1_21_10 + 93;

    public static void register() {
        MCTypeRegistry.DATA_COMPONENTS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType consumable = data.getMap("minecraft:consumable");
                if (consumable == null) {
                    return null;
                }

                final String animation = consumable.getString("animation");
                if ("spear".equals(animation)) {
                    consumable.setString("animation", "trident");
                    return null;
                }

                return null;
            }
        });
    }

    private V4649() {}
}
