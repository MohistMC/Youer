package com.mohistmc.youer.commands;

import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

/**
 * Forces recalculation of light for all loaded chunks and resends them to clients.
 * Use after lighting data corruption.
 *
 * Sub-commands:
 *   /lightfix           — Recalculate light for all loaded chunks
 *   /lightfix <world>   — Recalculate light for a specific world
 *   /lightfix corrupt   — Simulate light corruption (for testing)
 *
 * @author Mgazul
 */
public class LightFixCommand extends BukkitCommand {

    public LightFixCommand(String name) {
        super(name);
        this.description = I18n.as("lightfix.description");
        this.usageMessage = "/lightfix [world|corrupt]";
        this.setPermission("youer.command.lightfix");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("corrupt")) {
            return corruptLight(sender, args);
        }

        World targetWorld = null;
        if (args.length > 0) {
            targetWorld = Bukkit.getWorld(args[0]);
            if (targetWorld == null) {
                sender.sendMessage(I18n.as("lightfix.world.notfound", args[0]));
                return true;
            }
        }

        List<CompletableFuture<?>> allFutures = new ArrayList<>();
        int totalChunks = 0;
        record FixedChunks(ServerLevel level, Set<ChunkPos> positions) {}
        List<FixedChunks> fixedWorlds = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            if (targetWorld != null && !world.equals(targetWorld)) continue;

            ServerLevel nmsLevel = ((CraftWorld) world).getHandle();
            ThreadedLevelLightEngine lightEngine = nmsLevel.getChunkSource().getLightEngine();
            int worldChunks = 0;
            Set<ChunkPos> fixedPositions = new HashSet<>();

            var chunkMap = nmsLevel.getChunkSource().chunkMap;
            var chunks = new ArrayList<>(chunkMap.visibleChunkMap.long2ObjectEntrySet());

            int minSection = nmsLevel.getMinSection();
            int maxSection = nmsLevel.getMaxSection() - 1;

            for (var entry : chunks) {
                var holder = entry.getValue();
                ChunkAccess chunk = holder.getChunkIfPresent(ChunkStatus.FULL);
                if (!(chunk instanceof LevelChunk levelChunk)) continue;

                ChunkPos chunkPos = levelChunk.getPos();

                levelChunk.initializeLightSources();
                for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
                    SectionPos sp = SectionPos.of(chunkPos, sectionY);
                    lightEngine.queueSectionData(LightLayer.SKY, sp, new DataLayer());
                }
                levelChunk.setLightCorrect(false);

                CompletableFuture<ChunkAccess> initFuture = lightEngine.initializeLight(levelChunk, false);
                CompletableFuture<ChunkAccess> lightFuture = initFuture.thenCompose(
                    c -> lightEngine.lightChunk(c, false)
                );
                allFutures.add(lightFuture);
                fixedPositions.add(chunkPos);
                worldChunks++;
                totalChunks++;
            }

            if (!fixedPositions.isEmpty()) {
                fixedWorlds.add(new FixedChunks(nmsLevel, fixedPositions));
            }
            sender.sendMessage(I18n.as("lightfix.queued", worldChunks, world.getName()));
        }

        if (totalChunks == 0) {
            sender.sendMessage(I18n.as("lightfix.nochunks"));
            return true;
        }

        CompletableFuture<Void> allDone = CompletableFuture.allOf(
            allFutures.toArray(new CompletableFuture[0])
        );

        final int finalTotal = totalChunks;
        final List<FixedChunks> finalFixedWorlds = fixedWorlds;
        MinecraftServer server = MinecraftServer.getServer();
        allDone.thenRunAsync(() -> {
            int resendCount = 0;
            for (FixedChunks fixed : finalFixedWorlds) {
                ThreadedLevelLightEngine lightEngine = fixed.level.getChunkSource().getLightEngine();
                for (ServerPlayer player : fixed.level.players()) {
                    for (ChunkPos pos : fixed.positions) {
                        if (player.getChunkTrackingView().contains(pos)) {
                            player.connection.send(new ClientboundLightUpdatePacket(pos, lightEngine, null, null));
                            resendCount++;
                        }
                    }
                }
                fixed.level.getChunkSource().getLightEngine().tryScheduleUpdate();
            }
            sender.sendMessage(I18n.as("lightfix.done", finalTotal, resendCount));
            sender.sendMessage(I18n.as("lightfix.save"));
        }, server);

        sender.sendMessage(I18n.as("lightfix.start", totalChunks));
        return true;
    }

    private boolean corruptLight(CommandSender sender, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage(I18n.as("lightfix.corrupt.onlyplayer"));
            return true;
        }

        ServerLevel nmsLevel = ((CraftWorld) player.getWorld()).getHandle();
        ThreadedLevelLightEngine lightEngine = nmsLevel.getChunkSource().getLightEngine();
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        int radius = 1;

        int minSection = nmsLevel.getMinSection();
        int maxSection = nmsLevel.getMaxSection() - 1;
        List<LevelChunk> corruptedChunks = new ArrayList<>();
        Set<ChunkPos> corruptedPositions = new HashSet<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk levelChunk = nmsLevel.getChunkSource().getChunkAtIfLoadedImmediately(cx + dx, cz + dz);
                if (levelChunk == null) continue;
                ChunkPos chunkPos = levelChunk.getPos();

                for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
                    SectionPos sp = SectionPos.of(chunkPos, sectionY);
                    lightEngine.queueSectionData(LightLayer.BLOCK, sp, new DataLayer());
                    lightEngine.queueSectionData(LightLayer.SKY, sp, new DataLayer());
                }
                corruptedPositions.add(chunkPos);
                corruptedChunks.add(levelChunk);
            }
        }

        for (LevelChunk chunk : corruptedChunks) {
            CompletableFuture<ChunkAccess> f = lightEngine
                .initializeLight(chunk, true)
                .thenCompose(c -> lightEngine.lightChunk(c, true));
            futures.add(f);
        }

        final int totalChunks = corruptedChunks.size();
        final Set<ChunkPos> finalPositions = new HashSet<>(corruptedPositions);
        final ServerLevel finalLevel = nmsLevel;
        final ThreadedLevelLightEngine finalLightEngine = lightEngine;
        MinecraftServer server = MinecraftServer.getServer();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRunAsync(() -> {
                for (ChunkPos pos : finalPositions) {
                    LevelChunk lc = finalLevel.getChunkSource().getChunkAtIfLoadedImmediately(pos.x, pos.z);
                    if (lc != null) {
                        lc.setLightCorrect(true);
                        lc.setUnsaved(true);
                    }
                }
                int totalPackets = 0;
                for (ServerPlayer nmsPlayer : finalLevel.players()) {
                    for (ChunkPos pos : finalPositions) {
                        if (nmsPlayer.getChunkTrackingView().contains(pos)) {
                            nmsPlayer.connection.send(
                                new ClientboundLightUpdatePacket(pos, finalLightEngine, null, null));
                            totalPackets++;
                        }
                    }
                }
                sender.sendMessage(I18n.as("lightfix.corrupt.done", totalChunks, totalPackets));
            }, server);

        sender.sendMessage(I18n.as("lightfix.corrupt.start", totalChunks));
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            if ("corrupt".startsWith(prefix)) suggestions.add("corrupt");
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().startsWith(prefix)) {
                    suggestions.add(world.getName());
                }
            }
            return suggestions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("corrupt")) {
            List<String> suggestions = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().startsWith(prefix)) {
                    suggestions.add(world.getName());
                }
            }
            return suggestions;
        }
        return List.of();
    }
}
