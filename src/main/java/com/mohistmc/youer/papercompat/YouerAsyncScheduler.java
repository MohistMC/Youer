package com.mohistmc.youer.papercompat;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Non-Folia mapping: async tasks use {@link BukkitScheduler#runTaskLaterAsynchronously} (tick-based delay approximation).
 */
@SuppressWarnings("rawtypes")
public final class YouerAsyncScheduler implements AsyncScheduler {

    private final Server server;

    public YouerAsyncScheduler(Server server) {
        this.server = server;
    }

    private BukkitScheduler scheduler() {
        return server.getScheduler();
    }

    private static long toTicks(long amount, TimeUnit unit) {
        long ms = unit.toMillis(amount);
        if (ms <= 0L) {
            return 0L;
        }
        return Math.max(1L, (ms * 20L + 999L) / 1000L);
    }

    @Override
    public @NotNull ScheduledTask runNow(@NotNull Plugin plugin, @NotNull Consumer task) {
        BukkitBackedScheduledTask st = new BukkitBackedScheduledTask(plugin, false);
        BukkitTask bt = scheduler().runTaskAsynchronously(plugin, oneShotRunnable(st, task));
        st.setBukkitTask(bt);
        return st;
    }

    @Override
    public @NotNull ScheduledTask runDelayed(@NotNull Plugin plugin, @NotNull Consumer task, long delay, @NotNull TimeUnit unit) {
        BukkitBackedScheduledTask st = new BukkitBackedScheduledTask(plugin, false);
        long ticks = toTicks(delay, unit);
        BukkitTask bt = scheduler().runTaskLaterAsynchronously(plugin, oneShotRunnable(st, task), ticks);
        st.setBukkitTask(bt);
        return st;
    }

    @Override
    public @NotNull ScheduledTask runAtFixedRate(@NotNull Plugin plugin, @NotNull Consumer task,
            long initialDelay, long period, @NotNull TimeUnit unit) {
        BukkitBackedScheduledTask st = new BukkitBackedScheduledTask(plugin, true);
        long initialTicks = toTicks(initialDelay, unit);
        long periodTicks = Math.max(1L, toTicks(period, unit));
        BukkitTask bt = scheduler().runTaskTimerAsynchronously(plugin, () -> task.accept(st), initialTicks, periodTicks);
        st.setBukkitTask(bt);
        return st;
    }

    @Override
    public void cancelTasks(@NotNull Plugin plugin) {
        scheduler().cancelTasks(plugin);
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
