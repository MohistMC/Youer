package com.mohistmc.youer.commands;

import com.mohistmc.youer.util.I18n;
import com.mohistmc.youer.util.ThreadUtils;
import com.mohistmc.youer.util.TimeUtils;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.purpurmc.purpur.task.PregenBossBarTask;
import org.purpurmc.purpur.util.MinecraftInternalPlugin;

/**
 * Multi-threaded chunk pregeneration command.
 *
 * <p>Design:
 * <ul>
 *   <li>A fixed worker pool submits {@code ChunkStatus.FULL} load requests from a shared
 *       spiral-scanning cursor (center outward, streamed — O(1) memory regardless of area).</li>
 *   <li>A {@link Semaphore} caps how many chunks are in-flight, so chunk-map scheduling and the
 *       vanilla world-gen thread pool stay saturated without exhausting memory. Actual terrain/light
 *       generation runs inside the vanilla generation thread pool — the real parallelism.</li>
 *   <li>{@code getChunkFuture()} keeps a chunk alive with a {@link TicketType#UNKNOWN} ticket that
 *       expires after 1 tick. A main-thread {@link org.bukkit.scheduler.BukkitRunnable} (run once,
 *       one tick delay/period) renews that ticket for every in-flight chunk each tick, so chunks
 *       are never unloaded mid-generation.</li>
 *   <li>Completion callbacks are cheap (atomic counters + volatile dirty flag) and run on the
 *       completion thread; a scheduled task periodically saves chunks to disk.</li>
 *   <li>The progress bossbar is managed by {@link PregenBossBarTask} (modeled on TPSBarTask):
 *       the shared {@code BossBarTask} machinery handles per-tick rendering, removal on quit and
 *       re-show on rejoin via {@code PlayerList}.</li>
 * </ul>
 *
 * <p>Sub-commands:
 * <pre>
 *   /pregen start &lt;world&gt; &lt;radius&gt; [threads]             — circle around spawn/player
 *   /pregen start &lt;world&gt; &lt;x1&gt; &lt;z1&gt; &lt;x2&gt; &lt;z2&gt; [threads]  — rectangle
 *   /pregen status | pause | resume | stop | cancel
 * </pre>
 *
 * @author Mgazul
 */
public class PregenCommand extends BukkitCommand {

    private static final int MAX_THREADS = 64;
    private static final int SAVE_INTERVAL_SECONDS = 30;
    private static final AtomicReference<PregenTask> ACTIVE_TASK = new AtomicReference<>();
    private static final AtomicBoolean TICKER_REGISTERED = new AtomicBoolean();

    public PregenCommand(String name) {
        super(name);
        this.description = I18n.as("pregen.description");
        this.usageMessage = "/pregen <start|status|pause|resume|stop|cancel>";
        this.setPermission("youer.command.pregen");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(I18n.as("pregen.usage"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "start" -> start(sender, args);
            case "status" -> status(sender);
            case "pause" -> pause(sender);
            case "resume" -> resume(sender);
            case "stop" -> stop(sender, true);
            case "cancel" -> stop(sender, false);
            case "cleanup" -> cleanup(sender, args);
            default -> sender.sendMessage(I18n.as("pregen.usage"));
        }
        return true;
    }

    /**
     * Deletes 0-byte region files. RegionFile.open() creates the file on first touch but only
     * writes the 8192-byte header once a chunk is actually saved, so region files that were
     * touched without any chunk making it to disk stay at 0 KB. They hold no data and are
     * safe to remove; vanilla treats a missing file the same as an empty one.
     */
    private void cleanup(CommandSender sender, String[] args) {
        PregenTask task = ACTIVE_TASK.get();
        if (task != null && task.running) {
            sender.sendMessage(I18n.as("pregen.cleanup.onlywhenidle"));
            return;
        }
        World target = args.length >= 2 ? Bukkit.getWorld(args[1]) : null;
        if (args.length >= 2 && target == null) {
            sender.sendMessage(I18n.as("pregen.world.notfound", args[1]));
            return;
        }
        List<World> worlds = target != null ? List.of(target) : Bukkit.getWorlds();
        int removed = 0;
        for (World world : worlds) {
            File regionDir = new File(world.getWorldFolder(), "region");
            if (!regionDir.isDirectory()) {
                continue;
            }
            File[] files = regionDir.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".mca") && file.length() == 0) {
                    if (file.delete()) {
                        removed++;
                    }
                }
            }
        }
        sender.sendMessage(I18n.as("pregen.cleanup.done", removed));
    }

    private void start(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(I18n.as("pregen.usage"));
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            sender.sendMessage(I18n.as("pregen.world.notfound", args[1]));
            return;
        }
        ServerLevel level = ((CraftWorld) world).getHandle();

        PregenTask existing = ACTIVE_TASK.get();
        if (existing != null && existing.running) {
            sender.sendMessage(I18n.as("pregen.already.running"));
            return;
        }

        int threads = defaultThreads();
        boolean circle;
        int x1, z1, x2, z2, radius = 0;
        int centerX = 0, centerZ = 0;

        if (args.length == 3 || args.length == 4) {
            Integer radiusValue = parseInt(sender, args[2]);
            if (radiusValue == null) {
                return;
            }
            radius = radiusValue;
            if (radius <= 0) {
                sender.sendMessage(I18n.as("pregen.number.invalid", args[2]));
                return;
            }
            if (args.length == 4) {
                threads = parseThreads(sender, args[3]);
            }
            circle = true;
            // Always center on the world spawn point, never on the issuing player.
            BlockPos spawn = level.getSharedSpawnPos();
            centerX = spawn.getX() >> 4;
            centerZ = spawn.getZ() >> 4;
            x1 = centerX - radius;
            z1 = centerZ - radius;
            x2 = centerX + radius;
            z2 = centerZ + radius;
        } else if (args.length == 6 || args.length == 7) {
            Integer v1 = parseInt(sender, args[2]);
            Integer v2 = parseInt(sender, args[3]);
            Integer v3 = parseInt(sender, args[4]);
            Integer v4 = parseInt(sender, args[5]);
            if (v1 == null || v2 == null || v3 == null || v4 == null) {
                return;
            }
            x1 = Math.min(v1, v3);
            z1 = Math.min(v2, v4);
            x2 = Math.max(v1, v3);
            z2 = Math.max(v2, v4);
            if (args.length == 7) {
                threads = parseThreads(sender, args[6]);
            }
            circle = false;
        } else {
            sender.sendMessage(I18n.as("pregen.usage"));
            return;
        }

        long total = circle
                ? countCircleChunks(radius)
                : (long) (x2 - x1 + 1) * (z2 - z1 + 1);
        if (total <= 0) {
            sender.sendMessage(I18n.as("pregen.start.too.large", total));
            return;
        }
        // The only hard limit is the world border: with spawn-centered circles the radius may
        // reach the border. Check the area's bounding square against the border (circles are
        // inside their bounding square, so this is a conservative check).
        net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
        double halfChunks = border.getSize() / 32.0; // diameter / 2 / 16
        double borderX = border.getCenterX() / 16.0;
        double borderZ = border.getCenterZ() / 16.0;
        boolean insideBorder = circle
                ? centerX - radius >= borderX - halfChunks
                        && centerX + radius <= borderX + halfChunks
                        && centerZ - radius >= borderZ - halfChunks
                        && centerZ + radius <= borderZ + halfChunks
                : x1 >= borderX - halfChunks && x2 <= borderX + halfChunks
                        && z1 >= borderZ - halfChunks && z2 <= borderZ + halfChunks;
        if (!insideBorder) {
            sender.sendMessage(I18n.as("pregen.start.border", (int) Math.floor(halfChunks)));
            return;
        }

        // 1 per worker: each active generation task claims a radius-8 pyramid of holders
        // (~289 chunks) that must stay loaded, so in-flight depth directly sizes the holder
        // set (and thus per-tick chunk-map traversal + unload backlog). 1/worker keeps the
        // holder set ~18k (64 tasks x 289) so the server thread stays at 20 TPS.
        int inflightPermits = Math.min(threads, 128);
        PregenTask task = new PregenTask(level, world.getName(),
                circle ? new SpiralCursor(centerX, centerZ, radius) : new SpiralCursor(x1, z1, x2, z2),
                total, threads, inflightPermits, sender);

        if (!ACTIVE_TASK.compareAndSet(existing, task)) {
            sender.sendMessage(I18n.as("pregen.already.running"));
            return;
        }

        task.workers = Executors.newFixedThreadPool(threads, workerFactory("Youer-Pregen-Worker"));
        task.saver = Executors.newSingleThreadScheduledExecutor(workerFactory("Youer-Pregen-Save"));
        task.saver.scheduleWithFixedDelay(() -> {
            if (!task.running) {
                return;
            }
            ThreadUtils.executeOnMainThread(() -> task.level.getChunkSource().save(false));
        }, SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);

        if (sender instanceof Player player) {
            task.ownerUuid = player.getUniqueId();
            PregenBossBarTask bar = PregenBossBarTask.instance();
            bar.start();
            bar.setProgress(0.0F);
            bar.setTitle(I18n.as("pregen.bossbar.title", "0.00", "0.0", "?", world.getName()));
            bar.addPlayer(player);
        }
        ensureTicker();

        task.activeWorkers.set(threads);
        for (int i = 0; i < threads; i++) {
            task.workers.execute(() -> workerLoop(task));
        }

        if (circle) {
            sender.sendMessage(I18n.as("pregen.start.radius", world.getName(), radius, total, threads));
            sender.sendMessage(I18n.as("pregen.start.center", centerX, centerZ));
        } else {
            sender.sendMessage(I18n.as("pregen.start.rect", world.getName(), x1, z1, x2, z2, total, threads));
        }
        sender.sendMessage(I18n.as("pregen.save.scheduled", SAVE_INTERVAL_SECONDS));
    }

    private static void workerLoop(PregenTask task) {
        while (task.running) {
            if (task.paused) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }
            int[] pos = task.cursor.next();
            if (pos == null) {
                break;
            }
            long key = ChunkPos.asLong(pos[0], pos[1]);
            if (!task.phaseTwo) {
                // Phase 1 (structure starts): skip chunks that already exist on disk.
                if (regionHasChunk(task.level, pos[0], pos[1])) {
                    task.skipped.incrementAndGet();
                    continue;
                }
            } else {
                // Phase 2 (full generation): skip chunks that are already FULL on disk, but
                // NOT the ones this very task only generated to STRUCTURE_STARTS in phase 1.
                if (regionHasChunk(task.level, pos[0], pos[1]) && !task.phase1Chunks.contains(key)) {
                    task.skipped.incrementAndGet();
                    continue;
                }
            }
            task.inflight.acquireUninterruptibly();
            task.submitted.incrementAndGet();
            ChunkStatus target = task.phaseTwo ? ChunkStatus.FULL : task.phase1Status;
            task.inflightPositions.add(key);
            try {
                task.level.getChunkSource().getChunkFuture(pos[0], pos[1], target, true)
                        .whenComplete((result, ex) -> {
                            try {
                                if (ex != null) {
                                    recordFailure(task, "exception: " + ex);
                                    return;
                                }
                                if (result != null && result.isSuccess()) {
                                    ChunkAccess chunk = result.orElse(null);
                                    if (chunk != null) {
                                        // No setUnsaved here: freshly generated chunks are already
                                        // marked dirty by the generation pipeline (LevelChunk ctor),
                                        // while chunks loaded from disk must NOT be rewritten.
                                        if (!task.phaseTwo) {
                                            task.phase1Chunks.add(key);
                                        }
                                        task.completed.incrementAndGet();
                                        return;
                                    }
                                    recordFailure(task, "success with null chunk");
                                    return;
                                }
                                recordFailure(task, result != null ? result.getError() : "null result");
                            } finally {
                                task.inflightPositions.remove(key);
                                task.inflight.release();
                            }
                        });
            } catch (Throwable t) {
                // Never let a scheduling exception kill the worker / leak the permit.
                recordFailure(task, "scheduling: " + t);
                task.inflightPositions.remove(key);
                task.inflight.release();
            }
        }

        // Last worker to exit either advances to phase 2 or finishes the whole task.
        if (task.activeWorkers.decrementAndGet() == 0 && task.running) {
            if (!task.phaseTwo) {
                advancePhase(task);
            } else {
                finishTask(task, true);
            }
        }
    }

    /**
     * Phase 1 done: wait for the in-flight STRUCTURE_STARTS chunks, save them, then restart
     * the workers for phase 2 (full generation). Because every chunk now has its structure
     * starts ready, the 8-radius STRUCTURE_STARTS dependencies never block a worldgen thread.
     */
    private static void advancePhase(PregenTask task) {
        CompletableFuture.runAsync(() -> {
            waitForInflight(task);
            ThreadUtils.executeOnMainThread(() -> task.level.getChunkSource().save(false));
            waitForSerializations(task);
            task.level.getChunkSource().chunkMap.flushWorker();
            // Roll phase-1 counters into the totals, then restart the per-phase counters so
            // progress is always relative to the current phase (never over 100%).
            task.totalGenerated.addAndGet(task.completed.get());
            task.totalSkipped.addAndGet(task.skipped.get());
            task.totalFailed.addAndGet(task.failed.get());
            task.completed.set(0);
            task.skipped.set(0);
            task.failed.set(0);
            task.lastSampleCount.set(0);
            task.lastSampleAt.set(System.currentTimeMillis());
            task.cursor.reset();
            task.phaseTwo = true;
            task.activeWorkers.set(task.threads);
            for (int i = 0; i < task.threads; i++) {
                task.workers.execute(() -> workerLoop(task));
            }
        });
    }

    private static void finishTask(PregenTask task, boolean save) {
        if (!task.finished.compareAndSet(false, true)) {
            return;
        }
        task.running = false;
        task.saver.shutdown();
        // Do not cancel the bossbar task (a cancelled BukkitRunnable cannot be re-scheduled);
        // just remove the owner so the bar disappears. The task itself keeps running idle.
        if (task.ownerUuid != null) {
            Player owner = Bukkit.getPlayer(task.ownerUuid);
            if (owner != null) {
                PregenBossBarTask.instance().removePlayer(owner);
            }
        }
        if (save) {
            waitForInflight(task);
            // Submit every dirty chunk for serialization (main thread, fast: just enqueues).
            ThreadUtils.executeOnMainThread(() -> task.level.getChunkSource().save(false));
            // Wait until all off-thread serializations have finished...
            waitForSerializations(task);
            // ...then flush the IO worker so the data is actually on disk before we report done
            // (flushWorker joins the async IO queue; safe on a background thread).
            task.level.getChunkSource().chunkMap.flushWorker();
        }
        MinecraftServer server = MinecraftServer.getServer();
        long done = task.totalGenerated.get() + task.completed.get();
        long failed = task.totalFailed.get() + task.failed.get();
        long skipped = task.totalSkipped.get() + task.skipped.get();
        server.execute(() -> task.sender.sendMessage(
                I18n.as(save ? "pregen.finished" : "pregen.cancel.done", done, failed, skipped)));
        ACTIVE_TASK.set(null);
    }

    /** Runs on the main thread every server tick; renews the 1-tick UNKNOWN ticket of every in-flight chunk. */
    private static void refreshTickets(PregenTask task) {
        if (!task.running) {
            return;
        }
        ServerChunkCache chunkCache = task.level.getChunkSource();
        int level = ChunkLevel.byStatus(ChunkStatus.FULL);
        for (Long key : task.inflightPositions) {
            ChunkPos pos = new ChunkPos(key);
            chunkCache.chunkMap.distanceManager.addTicket(TicketType.UNKNOWN, pos, level, pos);
        }
    }

    private static void recordFailure(PregenTask task, String reason) {
        task.failed.incrementAndGet();
        task.failureReasons.computeIfAbsent(reason, k -> new AtomicInteger()).incrementAndGet();
    }

    /** Registers once: main-thread ticker that renews chunk tickets and pushes bossbar data. */
    private static void ensureTicker() {
        if (!TICKER_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                PregenTask task = ACTIVE_TASK.get();
                if (task == null) {
                    return;
                }
                // Keep refreshing tickets while in-flight chunks remain, even after stop():
                // the final wait must not let the 1-tick UNKNOWN tickets expire and fail the
                // last chunks. Only stop refreshing once everything has finished.
                if (!task.running && task.inflight.availablePermits() >= task.inflightPermits) {
                    return;
                }
                refreshTickets(task);
                if (task.running) {
                    updateBossBarData(task);
                }
            }
        }.runTaskTimer(new MinecraftInternalPlugin(), 1, 1);
    }

    private static void updateBossBarData(PregenTask task) {
        long done = task.completed.get();
        float pct = task.total > 0 ? (float) Math.min(1.0, done / (double) task.total) : 1.0f;
        long now = System.currentTimeMillis();
        double rate = task.sampleRate(now);
        long eta = rate > 0.5 ? (long) ((task.total - done) / rate) : -1;
        PregenBossBarTask bar = PregenBossBarTask.instance();
        bar.setProgress(pct);
        bar.setColor(colorFor(pct));
        String title = I18n.as("pregen.bossbar.title",
                String.format("%.2f", pct * 100),
                String.format("%.1f", rate),
                eta >= 0 ? TimeUtils.formatDuration(eta) : "?",
                task.worldName);
        if (task.failed.get() > 0) {
            title += I18n.as("pregen.bossbar.failed", task.failed.get());
        }
        if (task.paused) {
            title += I18n.as("pregen.bossbar.paused");
        }
        bar.setTitle(title);
    }

    /** True if {@code uuid} owns the currently running pregen task (used by BossBarTask.addToAll). */
    public static boolean isOwner(UUID uuid) {
        PregenTask task = ACTIVE_TASK.get();
        return task != null && task.running
                && task.ownerUuid != null && task.ownerUuid.equals(uuid);
    }

    private static BossBar.Color colorFor(float pct) {
        if (pct < 0.25f) {
            return BossBar.Color.RED;
        }
        if (pct < 0.5f) {
            return BossBar.Color.YELLOW;
        }
        if (pct < 0.75f) {
            return BossBar.Color.GREEN;
        }
        return BossBar.Color.BLUE;
    }

    private void status(CommandSender sender) {
        PregenTask task = ACTIVE_TASK.get();
        if (task == null || !task.running) {
            sender.sendMessage(I18n.as("pregen.notrunning"));
            return;
        }
        long done = task.completed.get();
        long skipped = task.skipped.get();
        long failed = task.failed.get();
        long processed = done + skipped;
        long now = System.currentTimeMillis();

        double rate = task.sampleRate(now);
        long eta = rate > 0.5 ? (long) ((task.total - processed) / rate) : -1;
        double pct = task.total > 0 ? processed * 100.0 / task.total : 100.0;

        sender.sendMessage(I18n.as("pregen.status.title"));
        sender.sendMessage(I18n.as("pregen.status.world", task.worldName));
        sender.sendMessage(I18n.as("pregen.status.progress", processed, task.total,
                String.format("%.2f", pct), progressBar(pct)));
        if (skipped > 0) {
            sender.sendMessage(I18n.as("pregen.status.skipped", skipped));
        }
        if (failed > 0) {
            sender.sendMessage(I18n.as("pregen.status.failed", failed));
        }
        if (!task.failureReasons.isEmpty()) {
            sender.sendMessage(I18n.as("pregen.status.failures"));
            task.failureReasons.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get()))
                    .limit(5)
                    .forEach(e -> sender.sendMessage(
                            I18n.as("pregen.status.failure", e.getValue().get(), e.getKey())));
        }
        sender.sendMessage(I18n.as("pregen.status.rate", String.format("%.1f", rate)));
        sender.sendMessage(I18n.as("pregen.status.tps", String.format("%.1f", Bukkit.getServer().getTPS()[0])));
        sender.sendMessage(I18n.as("pregen.status.eta",
                eta >= 0 ? TimeUtils.formatDuration(eta) : "?"));
        sender.sendMessage(I18n.as("pregen.status.workers", task.threads, task.inflightPermits));
        int inFlightNow = task.inflightPermits - task.inflight.availablePermits();
        sender.sendMessage(I18n.as("pregen.status.scheduler",
                task.level.getChunkSource().getPendingTasksCount(), inFlightNow, task.inflightPermits));
        sender.sendMessage(I18n.as(task.phaseTwo ? "pregen.status.phase2" : "pregen.status.phase1"));
        sender.sendMessage(I18n.as(task.paused ? "pregen.status.paused" : "pregen.status.running"));
    }

    private void pause(CommandSender sender) {
        PregenTask task = ACTIVE_TASK.get();
        if (task == null || !task.running) {
            sender.sendMessage(I18n.as("pregen.notrunning"));
            return;
        }
        task.paused = true;
        sender.sendMessage(I18n.as("pregen.paused"));
    }

    private void resume(CommandSender sender) {
        PregenTask task = ACTIVE_TASK.get();
        if (task == null || !task.running) {
            sender.sendMessage(I18n.as("pregen.notrunning"));
            return;
        }
        task.paused = false;
        sender.sendMessage(I18n.as("pregen.resumed"));
    }

    private void stop(CommandSender sender, boolean save) {
        PregenTask task = ACTIVE_TASK.get();
        if (task == null || !task.running) {
            sender.sendMessage(I18n.as("pregen.notrunning"));
            return;
        }
        task.running = false;
        task.saver.shutdown();
        int inflightCount = task.inflightPermits - task.inflight.availablePermits();
        sender.sendMessage(I18n.as(save ? "pregen.stop.stopping" : "pregen.cancel.stopping", inflightCount));

        CompletableFuture.runAsync(() -> {
            task.workers.shutdown();
            try {
                task.workers.awaitTermination(3, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            finishTask(task, save);
        });
    }

    private static void waitForInflight(PregenTask task) {
        long deadline = System.currentTimeMillis() + 120_000;
        while (System.currentTimeMillis() < deadline) {
            if (task.inflight.availablePermits() >= task.inflightPermits) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** Waits until every off-thread chunk serialization has completed (max 5 minutes). */
    private static void waitForSerializations(PregenTask task) {
        long deadline = System.currentTimeMillis() + 300_000;
        while (System.currentTimeMillis() < deadline
                && task.level.getChunkSource().chunkMap.pendingSerializations.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static String progressBar(double pct) {
        int bar = 24;
        int filled = Math.min(bar, Math.max(0, (int) (pct * bar / 100)));
        StringBuilder sb = new StringBuilder("&7[");
        for (int i = 0; i < bar; i++) {
            sb.append(i < filled ? "&a#" : "&8-");
        }
        sb.append("&7]");
        return sb.toString();
    }

    /**
     * True if the chunk already has data on disk. Reads the 4-byte big-endian offset entry
     * from the region file header (first 4096 bytes = chunk offset table, index x + z*32).
     * Never creates files, so this cannot produce 0-byte regions like a real region open can.
     */
    private static boolean regionHasChunk(ServerLevel level, int chunkX, int chunkZ) {
        int regionX = Math.floorDiv(chunkX, 32);
        int regionZ = Math.floorDiv(chunkZ, 32);
        File regionFile = new File(new File(((CraftWorld) level.getWorld()).getWorldFolder(), "region"),
                "r." + regionX + "." + regionZ + ".mca");
        if (!regionFile.isFile()) {
            return false;
        }
        try (RandomAccessFile raf = new RandomAccessFile(regionFile, "r")) {
            raf.seek(((chunkX & 31) + (chunkZ & 31) * 32) * 4L);
            return raf.readInt() != 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static long countCircleChunks(int radius) {
        long total = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            int half = (int) Math.sqrt((long) radius * radius - (long) dz * dz);
            total += 2L * half + 1;
        }
        return total;
    }

    private static int defaultThreads() {
        return Math.max(4, Math.min(MAX_THREADS, Runtime.getRuntime().availableProcessors() / 2));
    }

    private static Integer parseInt(CommandSender sender, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            sender.sendMessage(I18n.as("pregen.number.invalid", value));
            return null;
        }
    }

    private static int parseThreads(CommandSender sender, String value) {
        int threads;
        try {
            threads = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            sender.sendMessage(I18n.as("pregen.number.invalid", value));
            return defaultThreads();
        }
        if (threads < 1 || threads > MAX_THREADS) {
            sender.sendMessage(I18n.as("pregen.threads.invalid", value, MAX_THREADS));
            return defaultThreads();
        }
        return threads;
    }

    private static ThreadFactory workerFactory(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        };
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return List.of("start", "status", "pause", "resume", "stop", "cancel", "cleanup").stream()
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("cleanup"))) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    /**
     * Streams chunk coordinates in a square spiral starting from the center,
     * skipping coordinates outside the requested shape (circle or rectangle).
     * O(1) memory regardless of area size.
     */
    private static final class SpiralCursor {

        private static final int[][] DIRS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

        private final int x1, z1, x2, z2;
        private final int cx, cz;
        private final boolean circle;
        private final int radius;
        private final long scanTotal;

        private int x, z;
        private int dir;
        private int stepLen = 1;
        private int stepCount;
        private int turnCount;
        private boolean first = true;
        private long scanned;

        SpiralCursor(int cx, int cz, int radius) {
            this.circle = true;
            this.cx = cx;
            this.cz = cz;
            this.radius = radius;
            this.x1 = cx - radius;
            this.z1 = cz - radius;
            this.x2 = cx + radius;
            this.z2 = cz + radius;
            this.scanTotal = (long) (2 * radius + 1) * (2 * radius + 1);
        }

        SpiralCursor(int x1, int z1, int x2, int z2) {
            this.circle = false;
            this.x1 = x1;
            this.z1 = z1;
            this.x2 = x2;
            this.z2 = z2;
            this.cx = x1 + (x2 - x1) / 2;
            this.cz = z1 + (z2 - z1) / 2;
            this.radius = 0;
            int half = Math.max(x2 - cx, z2 - cz);
            this.scanTotal = (long) (2 * half + 1) * (2 * half + 1);
        }

        synchronized int[] next() {
            while (scanned < scanTotal) {
                scanned++;
                int px = cx + x;
                int pz = cz + z;
                step();
                if (inBounds(px, pz)) {
                    return new int[]{px, pz};
                }
            }
            return null;
        }

        synchronized void reset() {
            x = z = 0;
            dir = 0;
            stepLen = 1;
            stepCount = 0;
            turnCount = 0;
            first = true;
            scanned = 0;
        }

        private boolean inBounds(int px, int pz) {
            if (circle) {
                long dx = px - cx;
                long dz = pz - cz;
                return dx * dx + dz * dz <= (long) radius * radius;
            }
            return px >= x1 && px <= x2 && pz >= z1 && pz <= z2;
        }

        private void step() {
            if (first) {
                first = false;
                return;
            }
            x += DIRS[dir][0];
            z += DIRS[dir][1];
            if (++stepCount >= stepLen) {
                stepCount = 0;
                dir = (dir + 1) & 3;
                if ((++turnCount & 1) == 0) {
                    stepLen++;
                }
            }
        }
    }

    private static final class PregenTask {

        final ServerLevel level;
        final String worldName;
        final SpiralCursor cursor;
        final long total;
        final int threads;
        final int inflightPermits;
        final Semaphore inflight;
        final AtomicLong skipped = new AtomicLong();
        final AtomicLong submitted = new AtomicLong();
        final AtomicLong totalGenerated = new AtomicLong();
        final AtomicLong totalSkipped = new AtomicLong();
        final AtomicLong totalFailed = new AtomicLong();
        final ConcurrentHashMap.KeySetView<Long, Boolean> inflightPositions = ConcurrentHashMap.newKeySet();
        final ConcurrentHashMap<String, AtomicInteger> failureReasons = new ConcurrentHashMap<>();
        final AtomicLong completed = new AtomicLong();
        final AtomicLong failed = new AtomicLong();
        final AtomicInteger activeWorkers = new AtomicInteger();
        final long startedAt = System.currentTimeMillis();
        final AtomicLong lastSampleAt = new AtomicLong(startedAt);
        final AtomicLong lastSampleCount = new AtomicLong();
        volatile double lastRate;
        final CommandSender sender;
        final AtomicBoolean finished = new AtomicBoolean();
        UUID ownerUuid;
        volatile boolean paused;
        volatile boolean running = true;
        // Two-phase pregen: phase 1 generates STRUCTURE_STARTS for the whole area first (no
        // dependencies, fully parallel), phase 2 generates everything else. This removes the
        // 8-radius STRUCTURE_STARTS dependency stalls from the FEATURES..FULL chain.
        final ChunkStatus phase1Status = ChunkStatus.STRUCTURE_STARTS;
        volatile boolean phaseTwo;
        final ConcurrentHashMap.KeySetView<Long, Boolean> phase1Chunks = ConcurrentHashMap.newKeySet();
        ExecutorService workers;
        ScheduledExecutorService saver;

        /** Chunks processed per second (generated + skipped) over a sliding window; at least 5s must pass between samples. */
        double sampleRate(long now) {
            long deltaTime = now - lastSampleAt.get();
            if (deltaTime < 5000) {
                return lastRate;
            }
            long sampleCount = completed.get() + skipped.get();
            long deltaCount = sampleCount - lastSampleCount.getAndSet(sampleCount);
            lastSampleAt.set(now);
            lastRate = deltaCount * 1000.0 / deltaTime;
            return lastRate;
        }

        PregenTask(ServerLevel level, String worldName, SpiralCursor cursor, long total,
                   int threads, int inflightPermits, CommandSender sender) {
            this.level = level;
            this.worldName = worldName;
            this.cursor = cursor;
            this.total = total;
            this.threads = threads;
            this.inflightPermits = inflightPermits;
            this.inflight = new Semaphore(inflightPermits);
            this.sender = sender;
        }
    }

}
