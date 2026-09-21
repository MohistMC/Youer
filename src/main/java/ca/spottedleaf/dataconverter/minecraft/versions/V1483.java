package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.entity.ConverterAbstractEntityRename;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

public final class V1483 {

    private static final int VERSION = MCVersions.V18W16A;

    public static void register() {
        ConverterAbstractEntityRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:puffer_fish", "minecraft:pufferfish"
                )
        )::get);
        ConverterAbstractItemRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:puffer_fish_spawn_egg", "minecraft:pufferfish_spawn_egg"
                )
        )::get);

        MCTypeRegistry.ENTITY.copyWalkers(VERSION, "minecraft:puffer_fish", "minecraft:pufferfish");
    }

    private V1483() {}
}
