package com.mohistmc.youer.api;

import com.mohistmc.youer.plugins.world.utils.ConfigByWorlds;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.ChunkGenerator;

/**
 * @author Mgazul by MohistMC
 * @date 2023/6/14 14:49:40
 */
public class WorldAPI {

    public static Map<BlockPos, Entity> destroyBlockProgress = new HashMap<>();

    public static ServerLevel getServerLevel(World world) {
        return ((CraftWorld) world).getHandle();
    }

    public static String getWorldName(World world) {
        return ConfigByWorlds.config.getString("worlds." + world.getName() + ".name", world.getName());
    }

    public static class VoidGenerator extends ChunkGenerator {
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            ChunkData chunkData = this.createChunkData(world);

            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    biome.setBiome(x + i, z + j, Biome.THE_VOID);
                }
            }

            for (int y = 0; y < world.getMaxHeight(); y++) {
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        chunkData.setBlock(i, y, j, Material.AIR);
                    }
                }
            }

            return chunkData;
        }
    }

    public static class FlatGenerator extends ChunkGenerator {
        private static final Material[] DEFAULT_FLAT_LAYERS = {
                Material.BEDROCK,        // 底层基岩
                Material.DIRT,           // 3层泥土
                Material.DIRT,
                Material.DIRT,
                Material.GRASS_BLOCK     // 表层草方块
        };

        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            ChunkData chunkData = this.createChunkData(world);

            // 设置生物群系为平原
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    biome.setBiome(i, j, Biome.PLAINS);
                }
            }

            // 生成超平坦地形层
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    for (int layer = 0; layer < DEFAULT_FLAT_LAYERS.length; layer++) {
                        chunkData.setBlock(i, -64 + layer, j, DEFAULT_FLAT_LAYERS[layer]);
                    }
                    // 其余部分为空气
                    for (int y = -64 + DEFAULT_FLAT_LAYERS.length; y < world.getMaxHeight(); y++) {
                        if (y < -64 || y >= DEFAULT_FLAT_LAYERS.length - 64) {
                            chunkData.setBlock(i, y, j, Material.AIR);
                        }
                    }
                }
            }

            return chunkData;
        }
    }
}
