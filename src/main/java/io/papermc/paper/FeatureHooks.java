package io.papermc.paper;

import io.papermc.paper.command.PaperSubcommand;
import io.papermc.paper.command.subcommands.ChunkDebugCommand;
import io.papermc.paper.command.subcommands.FixLightCommand;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.Strategy;
import org.bukkit.Chunk;
import org.bukkit.World;

public final class FeatureHooks {

    // this includes non-accessible entities
    public static Iterable<Entity> getAllEntities(final net.minecraft.server.level.ServerLevel level) {
        return ((ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup)level.getEntities()).getAllMapped(); // Paper - rewrite chunk system
    }

    public static void setPlayerChunkUnloadDelay(final long ticks) {
        ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.setUnloadDelay(ticks); // Paper - rewrite chunk system
    }

    public static void registerPaperCommands(final Map<Set<String>, PaperSubcommand> commands) {
        commands.put(Set.of("fixlight"), new FixLightCommand()); // Paper - rewrite chunk system
        commands.put(Set.of("debug", "chunkinfo", "holderinfo"), new ChunkDebugCommand());  // Paper - rewrite chunk system
    }

    public static LevelChunkSection createSection(final PalettedContainerFactory palettedContainerFactory, final Level level, final ChunkPos chunkPos, final int chunkSection) {
        return new LevelChunkSection(palettedContainerFactory, level, chunkPos, chunkSection); // Paper - Anti-Xray - Add parameters
    }

    public static void sendChunkRefreshPackets(final List<ServerPlayer> playersInRange, final LevelChunk chunk) {
        // Paper start - Anti-Xray
        final Map<Object, ClientboundLevelChunkWithLightPacket> refreshPackets = new HashMap<>();
        for (final ServerPlayer player : playersInRange) {
            if (player.connection == null) continue;

            final Boolean shouldModify = chunk.getLevel().chunkPacketBlockController.shouldModify(player, chunk);
            player.connection.send(refreshPackets.computeIfAbsent(shouldModify, s -> { // Use connection to prevent creating firing event
                return new ClientboundLevelChunkWithLightPacket(chunk, chunk.getLevel().getLightEngine(), null, null, (Boolean) s);
            }));
            // Paper end - Anti-Xray
        }
    }

    public static PalettedContainer<BlockState> emptyPalettedBlockContainer() {
        return new PalettedContainer<>(Blocks.AIR.defaultBlockState(), Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY), null); // Paper - Anti-Xray - Add preset block states
    }

    public static Set<Long> getSentChunkKeys(final ServerPlayer player) {
        return LongSets.unmodifiable(player.moonrise$getChunkLoader().getSentChunksRaw().clone()); // Paper - rewrite chunk system
    }

    public static Set<Chunk> getSentChunks(final ServerPlayer player) {
        // Paper start - rewrite chunk system
        if (player.moonrise$getChunkLoader() == null) {
            return ObjectSets.EMPTY_SET;
        }
        final LongOpenHashSet rawChunkKeys = player.moonrise$getChunkLoader().getSentChunksRaw();
        final ObjectSet<org.bukkit.Chunk> chunks = new ObjectOpenHashSet<>(rawChunkKeys.size());
        final World world = player.level().getWorld();
        final LongIterator iter = player.moonrise$getChunkLoader().getSentChunksRaw().longIterator();
        while (iter.hasNext()) {
            chunks.add(world.getChunkAt(iter.nextLong(), false));
        }
        // Paper end - rewrite chunk system
        return ObjectSets.unmodifiable(chunks);
    }

    public static boolean isChunkSent(final ServerPlayer player, final long chunkKey) {
        // Paper start - rewrite chunk system
        return player.moonrise$getChunkLoader() != null && player.moonrise$getChunkLoader().getSentChunksRaw().contains(chunkKey);
        // Paper end - rewrite chunk system
    }

    public static boolean isCollidingWithWorldBorder(final Entity entity) {
        final net.minecraft.world.level.border.WorldBorder border = entity.level().getWorldBorder();
        return ca.spottedleaf.moonrise.patches.collisions.CollisionUtil.isCollidingWithBorder(border, entity.getBoundingBox().inflate(ca.spottedleaf.moonrise.patches.collisions.CollisionUtil.COLLISION_EPSILON)) && border.isInsideCloseToBorder(entity, entity.getBoundingBox()); // Paper - rewrite collision system
    }

    public static void dumpAllChunkLoadInfo(net.minecraft.server.MinecraftServer server, boolean isLongTimeout) {
        ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.dumpAllChunkLoadInfo(server, isLongTimeout); // Paper - rewrite chunk system
    }

    private static void dumpEntity(final Entity entity) {
    }

    public static org.bukkit.entity.Entity[] getChunkEntities(net.minecraft.server.level.ServerLevel level, int chunkX, int chunkZ) {
        return level.getChunkEntities(chunkX, chunkZ); // Paper - rewrite chunk system
    }

    public static java.util.Collection<org.bukkit.plugin.Plugin> getPluginChunkTickets(net.minecraft.server.level.ServerLevel level,
                                                                                       int x, int z) {
        return level.moonrise$getChunkTaskScheduler().chunkHolderManager.getPluginChunkTickets(x, z); // Paper - rewrite chunk system
    }

    public static Map<org.bukkit.plugin.Plugin, java.util.Collection<org.bukkit.Chunk>> getPluginChunkTickets(net.minecraft.server.level.ServerLevel level) {
        Map<org.bukkit.plugin.Plugin, com.google.common.collect.ImmutableList.Builder<Chunk>> ret = new HashMap<>();
        net.minecraft.server.level.DistanceManager chunkDistanceManager = level.getChunkSource().chunkMap.getDistanceManager();

        for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<java.util.Collection<net.minecraft.server.level.Ticket>> chunkTickets : chunkDistanceManager.moonrise$getChunkHolderManager().getTicketsCopy().long2ObjectEntrySet()) { // Paper - rewrite chunk system
            long chunkKey = chunkTickets.getLongKey();
            java.util.Collection<net.minecraft.server.level.Ticket> tickets = chunkTickets.getValue(); // Paper - rewrite chunk system

            org.bukkit.Chunk chunk = null;
            for (net.minecraft.server.level.Ticket ticket : tickets) {
                if (ticket.getType() != net.minecraft.server.level.TicketType.PLUGIN_TICKET) {
                    continue;
                }

                if (chunk == null) {
                    chunk = level.getWorld().getChunkAt(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey));
                }

                ret.computeIfAbsent((org.bukkit.plugin.Plugin) ticket.getIdentifier(), _ -> com.google.common.collect.ImmutableList.builder()).add(chunk);
            }
        }

        return ret.entrySet().stream().collect(com.google.common.collect.ImmutableMap.toImmutableMap(Map.Entry::getKey, (entry) -> entry.getValue().build()));
    }

    public static int getViewDistance(net.minecraft.server.level.ServerLevel level) {
        return level.moonrise$getPlayerChunkLoader().getAPIViewDistance(); // Paper - rewrite chunk system
    }

    public static int getSimulationDistance(net.minecraft.server.level.ServerLevel level) {
        return level.moonrise$getPlayerChunkLoader().getAPITickDistance(); // Paper - rewrite chunk system
    }

    public static int getSendViewDistance(net.minecraft.server.level.ServerLevel level) {
        return level.moonrise$getPlayerChunkLoader().getAPISendViewDistance(); // Paper - rewrite chunk system
    }

    public static void setViewDistance(net.minecraft.server.level.ServerLevel level, int distance) {
        if (distance < 2 || distance > 32) {
            throw new IllegalArgumentException("View distance " + distance + " is out of range of [2, 32]");
        }
        level.getChunkSource().chunkMap.setServerViewDistance(distance);
    }

    public static void setSimulationDistance(net.minecraft.server.level.ServerLevel level, int distance) {
        if (distance < 2 || distance > 32) {
            throw new IllegalArgumentException("Simulation distance " + distance + " is out of range of [2, 32]");
        }
        level.getChunkSource().chunkMap.getDistanceManager().updateSimulationDistance(distance);
    }

    public static void setSendViewDistance(net.minecraft.server.level.ServerLevel level, int distance) {
        level.getChunkSource().setSendViewDistance(distance); // Paper - rewrite chunk system
    }

    public static void tickEntityManager(net.minecraft.server.level.ServerLevel level) {
        // Paper - rewrite chunk system
    }

    public static void closeEntityManager(net.minecraft.server.level.ServerLevel level, boolean save) {
        // Paper - rewrite chunk system
    }

    public static java.util.concurrent.Executor getWorldgenExecutor() {
        return Runnable::run; // Paper - rewrite chunk system
    }

    public static void setViewDistance(ServerPlayer player, int distance) {
        ((ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer)player).moonrise$getViewDistanceHolder().setLoadViewDistance(distance == -1 ? distance : distance + 1); // Paper - rewrite chunk system
    }

    public static void setSimulationDistance(ServerPlayer player, int distance) {
        ((ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer)player).moonrise$getViewDistanceHolder().setTickViewDistance(distance); // Paper - rewrite chunk system
    }

    public static void setSendViewDistance(ServerPlayer player, int distance) {
        ((ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer)player).moonrise$getViewDistanceHolder().setSendViewDistance(distance); // Paper - rewrite chunk system
    }

    public static void flushAsyncAppenders() {
        // Paper start - add explicit flush method
        if (!(org.apache.logging.log4j.LogManager.getContext(false) instanceof org.apache.logging.log4j.core.LoggerContext context)) {
            return;
        }

        for (final org.apache.logging.log4j.core.Appender appender : context.getConfiguration().getAppenders().values()) {
            if (appender instanceof org.apache.logging.log4j.core.appender.AsyncAppender asyncAppender) {
                final boolean flushed = asyncAppender.flush(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (!flushed) {
                    net.minecraft.server.MinecraftServer.LOGGER.warn("Failed to flush log messages before plugin unload.");
                }
            }
        }
        // Paper end - add explicit flush method
    }
}