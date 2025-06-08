package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.blockname.ConverterAbstractBlockRename;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public final class V2690 {

    private static final int VERSION = MCVersions.V21W05A;

    private static final Map<String, String> RENAMES = new HashMap<>(
            ImmutableMap.<String, String>builder()
                    .put("minecraft:weathered_copper_block", "minecraft:oxidized_copper_block")
                    .put("minecraft:semi_weathered_copper_block", "minecraft:weathered_copper_block")
                    .put("minecraft:lightly_weathered_copper_block", "minecraft:exposed_copper_block")
                    .put("minecraft:weathered_cut_copper", "minecraft:oxidized_cut_copper")
                    .put("minecraft:semi_weathered_cut_copper", "minecraft:weathered_cut_copper")
                    .put("minecraft:lightly_weathered_cut_copper", "minecraft:exposed_cut_copper")
                    .put("minecraft:weathered_cut_copper_stairs", "minecraft:oxidized_cut_copper_stairs")
                    .put("minecraft:semi_weathered_cut_copper_stairs", "minecraft:weathered_cut_copper_stairs")
                    .put("minecraft:lightly_weathered_cut_copper_stairs", "minecraft:exposed_cut_copper_stairs")
                    .put("minecraft:weathered_cut_copper_slab", "minecraft:oxidized_cut_copper_slab")
                    .put("minecraft:semi_weathered_cut_copper_slab", "minecraft:weathered_cut_copper_slab")
                    .put("minecraft:lightly_weathered_cut_copper_slab", "minecraft:exposed_cut_copper_slab")
                    .put("minecraft:waxed_semi_weathered_copper", "minecraft:waxed_weathered_copper")
                    .put("minecraft:waxed_lightly_weathered_copper", "minecraft:waxed_exposed_copper")
                    .put("minecraft:waxed_semi_weathered_cut_copper", "minecraft:waxed_weathered_cut_copper")
                    .put("minecraft:waxed_lightly_weathered_cut_copper", "minecraft:waxed_exposed_cut_copper")
                    .put("minecraft:waxed_semi_weathered_cut_copper_stairs", "minecraft:waxed_weathered_cut_copper_stairs")
                    .put("minecraft:waxed_lightly_weathered_cut_copper_stairs", "minecraft:waxed_exposed_cut_copper_stairs")
                    .put("minecraft:waxed_semi_weathered_cut_copper_slab", "minecraft:waxed_weathered_cut_copper_slab")
                    .put("minecraft:waxed_lightly_weathered_cut_copper_slab", "minecraft:waxed_exposed_cut_copper_slab")
                    .build()
    );

    public static void register() {
        ConverterAbstractItemRename.register(VERSION, RENAMES::get);
        ConverterAbstractBlockRename.register(VERSION, RENAMES::get);
    }

    private V2690() {}
}
