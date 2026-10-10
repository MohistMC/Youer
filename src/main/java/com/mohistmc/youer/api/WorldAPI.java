package com.mohistmc.youer.api;

import com.mohistmc.youer.feature.world.WorldConfig;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.ChunkGenerator;

/**
 * World helpers, including the runtime display name override.
 *
 * @author Mgazul by MohistMC
 * @date 2023/6/14 14:49:40
 */
public class WorldAPI {

    // runtime display name override, never persisted to worlds.yml
    private static final Map<String, String> WORLD_NAMES = new ConcurrentHashMap<>();

    /**
     * @return the server level behind a Bukkit world
     */
    public static ServerLevel getServerLevel(World world) {
        return ((CraftWorld) world).getHandle();
    }

    /**
     * Resolves the display name of a world: runtime override first, then the
     * {@code worlds.<name>.name} entry of worlds.yml, then the real world name.
     *
     * @return the name shown to players, never null for a loaded world
     */
    public static String getWorldName(World world) {
        String name = world.getName();
        String override = WORLD_NAMES.get(name);
        return override != null ? override : WorldConfig.config.getString("worlds." + name + ".name", name);
    }

    /**
     * Overrides the display name of a world in memory, worlds.yml stays untouched.
     *
     * @param displayName the name to show, null or empty clears the override
     */
    public static void setWorldName(World world, String displayName) {
        setWorldName(world.getName(), displayName);
    }

    /**
     * Overrides the display name in memory, worlds.yml stays untouched.
     *
     * @param worldName the real world name
     * @param displayName the name to show, null or empty clears the override
     */
    public static void setWorldName(String worldName, String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            WORLD_NAMES.remove(worldName);
        } else {
            WORLD_NAMES.put(worldName, displayName);
        }
    }

    /**
     * Drops the runtime override so the world falls back to worlds.yml.
     */
    public static void removeWorldName(String worldName) {
        WORLD_NAMES.remove(worldName);
    }

    /**
     * @return a snapshot of the current runtime overrides, keyed by real world name
     */
    public static Map<String, String> getWorldNames() {
        return Map.copyOf(WORLD_NAMES);
    }

    /** Fills every chunk with air, used by the {@code void} world type. */
    public static class VoidGenerator extends ChunkGenerator {

        @Override
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

    /** Bedrock + dirt + grass floor, used by the {@code flat} world type. */
    public static class FlatGenerator extends ChunkGenerator {

        private static final Material[] DEFAULT_FLAT_LAYERS = {
                Material.BEDROCK,
                Material.DIRT,
                Material.DIRT,
                Material.DIRT,
                Material.GRASS_BLOCK
        };

        @Override
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            ChunkData chunkData = this.createChunkData(world);

            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    biome.setBiome(i, j, Biome.PLAINS);
                }
            }

            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    for (int layer = 0; layer < DEFAULT_FLAT_LAYERS.length; layer++) {
                        chunkData.setBlock(i, -64 + layer, j, DEFAULT_FLAT_LAYERS[layer]);
                    }
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
