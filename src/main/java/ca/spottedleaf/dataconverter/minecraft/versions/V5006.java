package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.util.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class V5006 {

    private static final int VERSION = MCVersions.V26_3_SNAPSHOT6 + 1;

    public static void register() {
        MCTypeRegistry.BLOCK_STATE.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public @Nullable Object convert(final Object input, final long sourceVersion, final long toVersion) {
                if (!(input instanceof MapType data)) {
                    return null;
                }

                RenameHelper.renameSingle(data, "Name", "id");
                RenameHelper.renameSingle(data, "Properties", "properties");
                return null;
            }
        });
    }

    private V5006() {}
}
