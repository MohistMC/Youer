package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class V5012 {

    private static final int VERSION = MCVersions.V26_3_SNAPSHOT9 + 1;

    private static final Map<String, String> RENAMES = Map.of(
        "minecraft:ocean_explorer_map", "minecraft:ocean_monument_map",
        "minecraft:swamp_explorer_map", "minecraft:swamp_hut_map",
        "minecraft:trial_explorer_map", "minecraft:buried_trial_chambers_map",
        "minecraft:woodland_explorer_map", "minecraft:woodland_mansion_map",
        "minecraft:jungle_explorer_map", "minecraft:jungle_pyramid_map"
    );

    public static void register() {
        ConverterAbstractItemRename.register(VERSION, RENAMES::get);
    }

    private V5012() {}
}
