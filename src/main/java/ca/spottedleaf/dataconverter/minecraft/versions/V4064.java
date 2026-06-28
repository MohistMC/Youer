package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4064 {

    private static final int VERSION = MCVersions.V24W36A + 1;

    public static void register() {
        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType components = data.getMap("components");
                if (components == null) {
                    return null;
                }

                if (components.hasKey("minecraft:fire_resistant")) {
                    components.remove("minecraft:fire_resistant");

                    final MapType damageResistant = components.getTypeUtil().createEmptyMap();
                    components.setMap("minecraft:damage_resistant", damageResistant);

                    damageResistant.setString("types", "#minecraft:is_fire");
                }

                return null;
            }
        });
    }

    private V4064() {}
}
