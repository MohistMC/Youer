package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.util.ComponentUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;

public final class V1803 {

    private static final int VERSION = MCVersions.V1_13_2 + 172;

    public static void register() {
        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(VERSION) {
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

                final ListType lore = display.getList("Lore", ObjectType.STRING);
                if (lore == null) {
                    return null;
                }

                for (int i = 0, len = lore.size(); i < len; ++i) {
                    lore.setString(i, ComponentUtils.createPlainTextComponent(lore.getString(i)));
                }

                return null;
            }
        });
    }

    private V1803() {}
}
