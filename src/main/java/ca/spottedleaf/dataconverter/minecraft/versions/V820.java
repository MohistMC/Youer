package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

public final class V820 {

    private static final int VERSION = MCVersions.V1_11 + 1;

    public static void register() {
        ConverterAbstractItemRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:totem", "minecraft:totem_of_undying"
                )
        )::get);
    }

    private V820() {}
}
