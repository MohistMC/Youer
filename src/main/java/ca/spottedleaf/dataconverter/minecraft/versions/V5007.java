package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class V5007 {

    private static final int VERSION = MCVersions.V26_3_SNAPSHOT6 + 2;

    public static void register() {
        MCTypeRegistry.DATA_COMPONENTS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final Object animation = data.getGeneric("minecraft:swing_animation");
                if (animation != null) {
                    data.remove("minecraft:swing_animation");
                    data.setGeneric("minecraft:attack_animation", animation);
                    data.setGeneric("minecraft:interact_animation", animation instanceof MapType map ? map.copy() : animation instanceof ListType list ? list.copy() : animation);
                }
                return null;
            }
        });
    }

    private V5007() {}
}
