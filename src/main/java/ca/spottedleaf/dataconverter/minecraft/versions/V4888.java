package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.attributes.ConverterAbstractPostV4307AttributesRename;

public final class V4888 {

    private static final int VERSION = MCVersions.V26_2_SNAPSHOT4 + 1;

    private static String rename(final String id) {
        return "minecraft:nameplate_distance".equals(id) ? "minecraft:name_tag_distance" : null;
    }

    public static void register() {
        ConverterAbstractPostV4307AttributesRename.register(VERSION, V4888::rename);
    }

    private V4888() {}
}
