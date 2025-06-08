package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.attributes.ConverterAbstractAttributesRename;
import java.util.HashMap;
import java.util.Map;

public final class V3814 {

    private static final int VERSION = MCVersions.V24W05B + 3;

    public static void register() {
        final Map<String, String> renames = new HashMap<>(
                Map.of("minecraft:horse.jump_strength", "minecraft:generic.jump_strength")
        );

        ConverterAbstractAttributesRename.register(VERSION, renames::get);
    }

    private V3814() {}
}
