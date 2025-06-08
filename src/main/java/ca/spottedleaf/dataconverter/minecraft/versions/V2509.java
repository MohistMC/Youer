package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.entity.ConverterAbstractEntityRename;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

public final class V2509 {

    private static final int VERSION = MCVersions.V20W08A + 2;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

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

        registerMob("minecraft:zombified_piglin");
    }

    private V2509() {}
}
