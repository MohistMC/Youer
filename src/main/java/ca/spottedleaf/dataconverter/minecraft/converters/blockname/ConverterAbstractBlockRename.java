package ca.spottedleaf.dataconverter.minecraft.converters.blockname;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.ConverterAbstractStringValueTypeRename;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import java.util.function.Function;

public final class ConverterAbstractBlockRename {

    private ConverterAbstractBlockRename() {}

    public static void register(final int version, final Function<String, String> renamer) {
        register(version, 0, renamer);
    }

    public static void register(final int version, final int subVersion, final Function<String, String> renamer) {
        ConverterAbstractStringValueTypeRename.register(version, subVersion, MCTypeRegistry.BLOCK_NAME, renamer);
        MCTypeRegistry.BLOCK_STATE.addStructureConverter(new DataConverter<>(version, subVersion) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final String name = data.getString("Name");
                if (name != null) {
                    final String converted = renamer.apply(name);
                    if (converted != null) {
                        data.setString("Name", converted);
                    }
                }
                return null;
            }
        });
        MCTypeRegistry.FLAT_BLOCK_STATE.addConverter(new DataConverter<>(version, subVersion) {
            @Override
            public Object convert(final Object data, final long sourceVersion, final long toVersion) {
                if (!(data instanceof String string)) {
                    return null;
                }

                if (string.isEmpty()) {
                    return null;
                }

                final int nbtStart1 = string.indexOf('[');
                final int nbtStart2 = string.indexOf('{');
                int stateNameEnd = string.length();
                if (nbtStart1 > 0) {
                    stateNameEnd = nbtStart1;
                }

                if (nbtStart2 > 0) {
                    stateNameEnd = Math.min(stateNameEnd, nbtStart2);
                }

                final String blockStateName = string.substring(0, stateNameEnd);
                final String converted = renamer.apply(blockStateName);
                if (converted == null) {
                    return null;
                }

                return converted.concat(string.substring(stateNameEnd));
            }
        });
    }
}
