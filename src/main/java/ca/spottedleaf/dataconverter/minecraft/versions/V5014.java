package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class V5014 {

    private static final int VERSION = MCVersions.V26_3_SNAPSHOT9 + 3;

    public static void register() {
        MCTypeRegistry.DATA_COMPONENTS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                data.remove("minecraft:block_transformer");
                return null;
            }
        });
    }

    private V5014() {}
}
