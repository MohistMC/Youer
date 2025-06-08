package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.blockname.ConverterAbstractBlockRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

public final class V2700 {

    private static final int VERSION = MCVersions.V21W10A + 1;

    public static void register() {
        ConverterAbstractBlockRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:cave_vines_head", "minecraft:cave_vines",
                        "minecraft:cave_vines_body", "minecraft:cave_vines_plant"
                )
        )::get);
    }

    private V2700() {}
}
