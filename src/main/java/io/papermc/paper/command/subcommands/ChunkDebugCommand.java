package io.papermc.paper.command.subcommands;

import ca.spottedleaf.moonrise.common.util.JsonUtil;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import io.papermc.paper.command.CommandUtil;
import io.papermc.paper.command.PaperSubcommand;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.BLUE;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

@DefaultQualifier(NonNull.class)
public final class ChunkDebugCommand implements PaperSubcommand {
    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        switch (subCommand) {
            case "debug" -> this.doDebug(sender, args);
            case "chunkinfo" -> this.doChunkInfo(sender, args);
            case "holderinfo" -> this.doHolderInfo(sender, args);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String subCommand, final String[] args) {
        return switch (subCommand) {
            case "debug":
                if (args.length == 1) {
                    yield CommandUtil.getListMatchingLast(sender, args, "help", "chunks");
                }
            case "chunkinfo", "holderinfo":
                if (args.length >= 1 && (args.length == 1 || !args[0].equals("*"))) {
                    yield CommandUtil.getListMatchingLast(
                        sender,
                        args,
                        CommandUtil.getWorldSuggestions(sender.getServer(), args.length == 1)
                    );
                }
            default:
                yield Collections.emptyList();
        };
    }

    private void doChunkInfo(final CommandSender sender, final String[] args) {
        List<World> worlds;
        if (args.length < 1 || args[0].equals("*")) {
            worlds = Bukkit.getServer().getWorlds();
        } else {
            worlds = new ArrayList<>(args.length);
            for (final String arg : args) {
                @Nullable NamespacedKey key = NamespacedKey.fromString(arg);
                @Nullable World world = key == null ? null : sender.getServer().getWorld(key);
                if (world == null) {
                    sender.sendMessage(text("World '" + arg + "' is invalid", RED));
                    return;
                }
                worlds.add(world);
            }
        }

        int accumulatedTotal = 0;
        int accumulatedInactive = 0;
        int accumulatedBorder = 0;
        int accumulatedTicking = 0;
        int accumulatedEntityTicking = 0;

        for (final World bukkitWorld : worlds) {
            final ServerLevel world = ((CraftWorld) bukkitWorld).getHandle();

            int total = 0;
            int inactive = 0;
            int full = 0;
            int blockTicking = 0;
            int entityTicking = 0;

            for (final NewChunkHolder holder : ((ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolders()) {
                final NewChunkHolder.ChunkCompletion completion = holder.getLastChunkCompletion();
                final @Nullable ChunkAccess chunk = completion == null ? null : completion.chunk();

                if (!(chunk instanceof LevelChunk fullChunk)) {
                    continue;
                }

                ++total;

                switch (holder.getChunkStatus()) {
                    case INACCESSIBLE: {
                        ++inactive;
                        break;
                    }
                    case FULL: {
                        ++full;
                        break;
                    }
                    case BLOCK_TICKING: {
                        ++blockTicking;
                        break;
                    }
                    case ENTITY_TICKING: {
                        ++entityTicking;
                        break;
                    }
                }
            }

            accumulatedTotal += total;
            accumulatedInactive += inactive;
            accumulatedBorder += full;
            accumulatedTicking += blockTicking;
            accumulatedEntityTicking += entityTicking;

            sender.sendMessage(text().append(text("Chunks in ", BLUE), text(bukkitWorld.key().asString(), GREEN), text(":")));
            sender.sendMessage(text().color(DARK_AQUA).append(
                text("Total: ", BLUE), text(total),
                text(" Inactive: ", BLUE), text(inactive),
                text(" Full: ", BLUE), text(full),
                text(" Block Ticking: ", BLUE), text(blockTicking),
                text(" Entity Ticking: ", BLUE), text(entityTicking)
            ));
        }
        if (worlds.size() > 1) {
            sender.sendMessage(text().append(text("Chunks in ", BLUE), text("all listed worlds", GREEN), text(":", DARK_AQUA)));
            sender.sendMessage(text().color(DARK_AQUA).append(
                text("Total: ", BLUE), text(accumulatedTotal),
                text(" Inactive: ", BLUE), text(accumulatedInactive),
                text(" Full: ", BLUE), text(accumulatedBorder),
                text(" Block Ticking: ", BLUE), text(accumulatedTicking),
                text(" Entity Ticking: ", BLUE), text(accumulatedEntityTicking)
            ));
        }
    }

    private void doHolderInfo(final CommandSender sender, final String[] args) {
        List<World> worlds;
        if (args.length < 1 || args[0].equals("*")) {
            worlds = Bukkit.getServer().getWorlds();
        } else {
            worlds = new ArrayList<>(args.length);
            for (final String arg : args) {
                @Nullable NamespacedKey key = NamespacedKey.fromString(arg);
                @Nullable World world = key == null ? null : sender.getServer().getWorld(key);
                if (world == null) {
                    sender.sendMessage(text("World '" + arg + "' is invalid", RED));
                    return;
                }
                worlds.add(world);
            }
        }

        int accumulatedTotal = 0;
        int accumulatedCanUnload = 0;
        int accumulatedNull = 0;
        int accumulatedReadOnly = 0;
        int accumulatedProtoChunk = 0;
        int accumulatedFullChunk = 0;

        for (final World bukkitWorld : worlds) {
            final ServerLevel world = ((CraftWorld) bukkitWorld).getHandle();

            int total = 0;
            int canUnload = 0;
            int nullChunks = 0;
            int readOnly = 0;
            int protoChunk = 0;
            int fullChunk = 0;

            for (final NewChunkHolder holder : ((ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolders()) {
                final NewChunkHolder.ChunkCompletion completion = holder.getLastChunkCompletion();
                final @Nullable ChunkAccess chunk = completion == null ? null : completion.chunk();

                ++total;

                if (chunk == null) {
                    ++nullChunks;
                } else if (chunk instanceof ImposterProtoChunk) {
                    ++readOnly;
                } else if (chunk instanceof ProtoChunk) {
                    ++protoChunk;
                } else if (chunk instanceof LevelChunk) {
                    ++fullChunk;
                }

                if (holder.isSafeToUnload() == null) {
                    ++canUnload;
                }
            }

            accumulatedTotal += total;
            accumulatedCanUnload += canUnload;
            accumulatedNull += nullChunks;
            accumulatedReadOnly += readOnly;
            accumulatedProtoChunk += protoChunk;
            accumulatedFullChunk += fullChunk;

            sender.sendMessage(text().append(text("Chunks in ", BLUE), text(bukkitWorld.key().asString(), GREEN), text(":")));
            sender.sendMessage(text().color(DARK_AQUA).append(
                text("Total: ", BLUE), text(total),
                text(" Unloadable: ", BLUE), text(canUnload),
                text(" Null: ", BLUE), text(nullChunks),
                text(" ReadOnly: ", BLUE), text(readOnly),
                text(" Proto: ", BLUE), text(protoChunk),
                text(" Full: ", BLUE), text(fullChunk)
            ));
        }
        if (worlds.size() > 1) {
            sender.sendMessage(text().append(text("Chunks in ", BLUE), text("all listed worlds", GREEN), text(":", DARK_AQUA)));
            sender.sendMessage(text().color(DARK_AQUA).append(
                text("Total: ", BLUE), text(accumulatedTotal),
                text(" Unloadable: ", BLUE), text(accumulatedCanUnload),
                text(" Null: ", BLUE), text(accumulatedNull),
                text(" ReadOnly: ", BLUE), text(accumulatedReadOnly),
                text(" Proto: ", BLUE), text(accumulatedProtoChunk),
                text(" Full: ", BLUE), text(accumulatedFullChunk)
            ));
        }
    }

    private void doDebug(final CommandSender sender, final String[] args) {
        if (args.length < 1) {
            sender.sendMessage(text("Use /paper debug [chunks] help for more information on a specific command", RED));
            return;
        }

        final String debugType = args[0].toLowerCase(Locale.ROOT);
        switch (debugType) {
            case "chunks" -> {
                if (args.length >= 2 && args[1].toLowerCase(Locale.ROOT).equals("help")) {
                    sender.sendMessage(text("Use /paper debug chunks to dump loaded chunk information to a file", RED));
                    break;
                }
                final File file = ChunkTaskScheduler.getChunkDebugFile();
                sender.sendMessage(text("Writing chunk information dump to " + file, GREEN));
                try {
                    JsonUtil.writeJson(ChunkTaskScheduler.debugAllWorlds(MinecraftServer.getServer()), file);
                    sender.sendMessage(text("Successfully written chunk information!", GREEN));
                } catch (Throwable thr) {
                    MinecraftServer.LOGGER.warn("Failed to dump chunk information to file " + file.toString(), thr);
                    sender.sendMessage(text("Failed to dump chunk information, see console", RED));
                }
            }
            // "help" & default
            default -> sender.sendMessage(text("Use /paper debug [chunks] help for more information on a specific command", RED));
        }
    }

}
