package com.mohistmc.youer.neoforge.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import terrablender.util.LevelUtils;

/**
 * @author Mgazul
 * @date 2026/6/12 02:46
 */
public class TerraBlenderCompat {

    public static void initializeBiomes(RegistryAccess registryAccess, Holder<DimensionType> dimensionType, ResourceKey<LevelStem> levelResourceKey, ChunkGenerator chunkGenerator, long seed) {
        LevelUtils.initializeBiomes(registryAccess, dimensionType, levelResourceKey, chunkGenerator, seed);
    }
}
