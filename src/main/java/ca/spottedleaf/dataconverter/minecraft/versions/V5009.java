package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class V5009 {

    private static final int VERSION = MCVersions.V26_3_SNAPSHOT7;

    public static void register() {
        MCTypeRegistry.CHUNK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType blending = data.getMap("blending_data");
                if (blending == null || !blending.hasKey("heights")) {
                    return null;
                }
                final ListType heights = blending.getListUnchecked("heights");
                final ListType converted = data.createEmptyList();
                if (heights != null) {
                    for (int i = 0, len = heights.size(); i < len; ++i) {
                        final double height = heights.getDouble(i, Double.MAX_VALUE);
                        converted.addFloat(height == Double.MAX_VALUE ? Float.MAX_VALUE : (float)height);
                    }
                }
                blending.setList("heights", converted);
                return null;
            }
        });

        MCTypeRegistry.ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType brain = data.getMap("Brain");
                if (brain == null) {
                    return null;
                }

                final MapType memories = brain.getMap("memories");
                if (memories == null) {
                    return null;
                }

                memories.remove("minecraft:is_tempted");
                return null;
            }
        });
    }

    private V5009() {}
}
