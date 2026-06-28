package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.blockname.ConverterAbstractBlockRename;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

public final class V2680 {

    private static final int VERSION = MCVersions.V1_16_5 + 94;

    public static void register() {
        ConverterAbstractItemRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:grass_path", "minecraft:dirt_path"
                )
        )::get);
        ConverterAbstractBlockRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:grass_path", "minecraft:dirt_path"
                )
        )::get);
    }

    private V2680() {}
}
