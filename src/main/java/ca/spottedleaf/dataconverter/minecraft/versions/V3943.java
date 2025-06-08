package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V3943 {

    private static final int VERSION = MCVersions.V2419WB + 1;

    public static void register() {
        MCTypeRegistry.OPTIONS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final String oldRange = data.getString("menuBackgroundBlurriness", "0.5");

                int newRange;
                try {
                    newRange = (int)Math.round(Double.parseDouble(oldRange) * 10.0);
                } catch (final NumberFormatException ex) {
                    newRange = 5;
                }

                // note: options are always string, so DFU is wrong to use int
                data.setString("menuBackgroundBlurriness", Integer.toString(newRange));

                return null;
            }
        });
    }

    private V3943() {}
}
