package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.stats.ConverterAbstractStatsRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

public final class V2710 {

    private static final int VERSION = MCVersions.V21W15A + 1;

    public static void register() {
        ConverterAbstractStatsRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "minecraft:play_one_minute", "minecraft:play_time"
                )
        )::get);
    }

    private V2710() {}
}
