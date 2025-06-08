package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V1948 {

    private static final int VERSION = MCVersions.V1_14_PRE2;

    public static void register() {
        MCTypeRegistry.ITEM_STACK.addConverterForId("minecraft:white_banner", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> tag = data.getMap("tag");
                if (tag == null) {
                    return null;
                }

                final MapType<String> display = tag.getMap("display");
                if (display == null) {
                    return null;
                }

                final String name = display.getString("Name");
                if (name == null) {
                    return null;
                }

                display.setString("Name", name.replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\""));

                return null;
            }
        });
    }

    private V1948() {}
}
