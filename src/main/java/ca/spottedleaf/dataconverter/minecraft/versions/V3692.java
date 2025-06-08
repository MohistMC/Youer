package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.blockname.ConverterAbstractBlockRename;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import java.util.HashMap;
import java.util.Map;

public final class V3692 {

    private static final int VERSION = MCVersions.V23W46A + 1;

    private static final Map<String, String> GRASS_RENAME = new HashMap<>(
            Map.of(
                    "minecraft:grass", "minecraft:short_grass"
            )
    );

    public static void register() {
        ConverterAbstractBlockRename.register(VERSION, GRASS_RENAME::get);
        ConverterAbstractItemRename.register(VERSION, GRASS_RENAME::get);
    }

    private V3692() {}
}
