package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.entity.ConverterAbstractEntityRename;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

public final class V2509 {

    private static final int VERSION = MCVersions.V20W08A + 2;

    public static void register() {
        ConverterAbstractItemRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:zombie_pigman_spawn_egg", "minecraft:zombified_piglin_spawn_egg"
                )
        )::get);
        ConverterAbstractEntityRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:zombie_pigman", "minecraft:zombified_piglin"
                )
        )::get);

        //registerMob("minecraft:zombified_piglin"); // changed to simple in 1.21.5
    }

    private V2509() {}
}
