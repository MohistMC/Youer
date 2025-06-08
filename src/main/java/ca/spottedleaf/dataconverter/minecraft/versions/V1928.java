package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.entity.ConverterAbstractEntityRename;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

public final class V1928 {

    private static final int VERSION = MCVersions.V19W04B + 1;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        ConverterAbstractEntityRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:illager_beast", "minecraft:ravager"
                )
        )::get);
        ConverterAbstractItemRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:illager_beast_spawn_egg", "minecraft:ravager_spawn_egg"
                )
        )::get);

        registerMob("minecraft:ravager");
    }

    private V1928() {}
}
