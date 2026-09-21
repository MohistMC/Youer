package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.advancements.ConverterAbstractAdvancementsRename;
import ca.spottedleaf.dataconverter.minecraft.converters.recipe.ConverterAbstractRecipeRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public final class V4769 {

    private static final int VERSION = MCVersions.V26_1_SNAPSHOT4 + 1;

    public static final Map<String, String> RECIPES_UPDATES = new HashMap<>(
        ImmutableMap.<String, String>builder()
            .put("minecraft:chiseled_stone_bricks_stone_from_stonecutting", "minecraft:chiseled_stone_bricks_from_stone_stonecutting")
            .put("minecraft:end_stone_brick_slab_from_end_stone_brick_stonecutting", "minecraft:end_stone_brick_slab_from_end_stone_bricks_stonecutting")
            .put("minecraft:end_stone_brick_stairs_from_end_stone_brick_stonecutting", "minecraft:end_stone_brick_stairs_from_end_stone_bricks_stonecutting")
            .put("minecraft:end_stone_brick_wall_from_end_stone_brick_stonecutting", "minecraft:end_stone_brick_wall_from_end_stone_bricks_stonecutting")
            .put("minecraft:mossy_stone_brick_slab_from_mossy_stone_brick_stonecutting", "minecraft:mossy_stone_brick_slab_from_mossy_stone_bricks_stonecutting")
            .put("minecraft:mossy_stone_brick_stairs_from_mossy_stone_brick_stonecutting", "minecraft:mossy_stone_brick_stairs_from_mossy_stone_bricks_stonecutting")
            .put("minecraft:mossy_stone_brick_wall_from_mossy_stone_brick_stonecutting", "minecraft:mossy_stone_brick_wall_from_mossy_stone_bricks_stonecutting")
            .put("minecraft:prismarine_brick_slab_from_prismarine_stonecutting", "minecraft:prismarine_brick_slab_from_prismarine_bricks_stonecutting")
            .put("minecraft:prismarine_brick_stairs_from_prismarine_stonecutting", "minecraft:prismarine_brick_stairs_from_prismarine_bricks_stonecutting")
            .put("minecraft:quartz_slab_from_stonecutting", "minecraft:quartz_slab_from_quartz_block_stonecutting")
            .put("minecraft:stone_brick_walls_from_stone_stonecutting", "minecraft:stone_brick_wall_from_stone_stonecutting")
            .build()
    );
    public static final Map<String, String> ADVANCEMENT_UPDATES = new HashMap<>(
        ImmutableMap.<String, String>builder()
            .put("minecraft:recipes/building_blocks/chiseled_stone_bricks_stone_from_stonecutting", "minecraft:recipes/building_blocks/chiseled_stone_bricks_from_stone_stonecutting")
            .put("minecraft:recipes/building_blocks/end_stone_brick_slab_from_end_stone_brick_stonecutting", "minecraft:recipes/building_blocks/end_stone_brick_slab_from_end_stone_bricks_stonecutting")
            .put("minecraft:recipes/building_blocks/end_stone_brick_stairs_from_end_stone_brick_stonecutting", "minecraft:recipes/building_blocks/end_stone_brick_stairs_from_end_stone_bricks_stonecutting")
            .put("minecraft:recipes/decorations/end_stone_brick_wall_from_end_stone_brick_stonecutting", "minecraft:recipes/decorations/end_stone_brick_wall_from_end_stone_bricks_stonecutting")
            .put("minecraft:recipes/building_blocks/mossy_stone_brick_slab_from_mossy_stone_brick_stonecutting", "minecraft:recipes/building_blocks/mossy_stone_brick_slab_from_mossy_stone_bricks_stonecutting")
            .put("minecraft:recipes/building_blocks/mossy_stone_brick_stairs_from_mossy_stone_brick_stonecutting", "minecraft:recipes/building_blocks/mossy_stone_brick_stairs_from_mossy_stone_bricks_stonecutting")
            .put("minecraft:recipes/building_blocks/mossy_stone_brick_wall_from_mossy_stone_brick_stonecutting", "minecraft:recipes/decorations/mossy_stone_brick_wall_from_mossy_stone_bricks_stonecutting")
            .put("minecraft:recipes/building_blocks/prismarine_brick_slab_from_prismarine_stonecutting", "minecraft:recipes/building_blocks/prismarine_brick_slab_from_prismarine_bricks_stonecutting")
            .put("minecraft:recipes/building_blocks/prismarine_brick_stairs_from_prismarine_stonecutting", "minecraft:recipes/building_blocks/prismarine_brick_stairs_from_prismarine_bricks_stonecutting")
            .put("minecraft:recipes/building_blocks/quartz_slab_from_stonecutting", "minecraft:recipes/building_blocks/quartz_slab_from_quartz_block_stonecutting")
            .put("minecraft:recipes/decorations/stone_brick_walls_from_stone_stonecutting", "minecraft:recipes/decorations/stone_brick_wall_from_stone_stonecutting")
            .build()
    );

    public static void register() {
        ConverterAbstractRecipeRename.register(VERSION, RECIPES_UPDATES::get);
        ConverterAbstractAdvancementsRename.register(VERSION, ADVANCEMENT_UPDATES::get);
    }

    private V4769() {}
}
