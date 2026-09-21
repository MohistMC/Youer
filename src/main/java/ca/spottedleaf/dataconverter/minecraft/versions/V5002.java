package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class V5002 {

    private static final int VERSION = MCVersions.V26_3_SNAPSHOT3 + 1;

    public static void register() {
        final DataConverter<MapType, MapType> converter = new DataConverter<>(VERSION) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                data.setBoolean("allow_op_features", true);
                return null;
            }
        };
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:sign", converter);
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:hanging_sign", converter);
    }

    private V5002() {}
}
