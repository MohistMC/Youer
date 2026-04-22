package com.mohistmc.youer.papercompat;

import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.function.Consumer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Non-Folia mapping: region tasks run on the main server thread via {@link BukkitScheduler}.
 */
@SuppressWarnings("rawtypes")
public final class YouerRegionScheduler implements RegionScheduler {

    private final Server server;

    public YouerRegionScheduler(Server server) {
        this.server = server;
    }

    private BukkitScheduler scheduler() {
        return server.getScheduler();
    }

    @Override
    public void execute(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Runnable run) {
        scheduler().runTask(plugin, run);
    }

    @Override
    public @NotNull ScheduledTask run(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Consumer task) {
        BukkitBackedScheduledTask st = new BukkitBackedScheduledTask(plugin, false);
        BukkitTask bt = scheduler().runTask(plugin, oneShotRunnable(st, task));
        st.setBukkitTask(bt);
        return st;
    }

    @Override
    public @NotNull ScheduledTask runDelayed(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Consumer task,
            long delayTicks) {
        BukkitBackedScheduledTask st = new BukkitBackedScheduledTask(plugin, false);
        BukkitTask bt = scheduler().runTaskLater(plugin, oneShotRunnable(st, task), Math.max(0L, delayTicks));
        st.setBukkitTask(bt);
        return st;
    }

    @Override
    public @NotNull ScheduledTask runAtFixedRate(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Consumer task,
            long initialDelayTicks, long periodTicks) {
        BukkitBackedScheduledTask st = new BukkitBackedScheduledTask(plugin, true);
        long period = Math.max(1L, periodTicks);
        BukkitTask bt = scheduler().runTaskTimer(plugin, () -> task.accept(st), Math.max(0L, initialDelayTicks), period);
        st.setBukkitTask(bt);
        return st;
    }

    private static Runnable oneShotRunnable(BukkitBackedScheduledTask st, Consumer task) {
        return () -> {
            try {
                task.accept(st);
            } finally {
                st.markOneShotFinished();
            }
        };
    }
}
