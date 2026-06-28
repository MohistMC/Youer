package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.blockname.ConverterAbstractBlockRename;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

public final class V2528 {

    private static final int VERSION = MCVersions.V20W16A + 2;

    public static void register() {
        ConverterAbstractItemRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:soul_fire_torch", "minecraft:soul_torch",
                        "minecraft:soul_fire_lantern", "minecraft:soul_lantern"
                )
        )::get);
        ConverterAbstractBlockRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:soul_fire_torch", "minecraft:soul_torch",
                        "minecraft:soul_fire_wall_torch", "minecraft:soul_wall_torch",
                        "minecraft:soul_fire_lantern", "minecraft:soul_lantern"
                )
        )::get);
    }

    private V2528() {}
}
