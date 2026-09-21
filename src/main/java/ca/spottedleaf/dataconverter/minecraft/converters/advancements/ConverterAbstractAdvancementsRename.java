package ca.spottedleaf.dataconverter.minecraft.converters.advancements;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.util.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import java.util.function.Function;

public final class ConverterAbstractAdvancementsRename {

    private ConverterAbstractAdvancementsRename() {}

    public static void register(final int version, final Function<String, String> renamer) {
        register(version, 0, renamer);
    }

    public static void register(final int version, final int subVersion, final Function<String, String> renamer) {
        MCTypeRegistry.ADVANCEMENTS.addStructureConverter(new DataConverter<>(version, subVersion) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                RenameHelper.renameKeys(data, renamer);
                return null;
            }
        });
    }

}
