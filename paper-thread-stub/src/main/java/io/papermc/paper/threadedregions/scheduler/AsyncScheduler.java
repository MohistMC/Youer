package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Paper Folia async scheduler API surface (stub JAR for plugin bytecode compatibility).
 */
public interface AsyncScheduler {

    @NotNull
    ScheduledTask runNow(@NotNull Plugin plugin, @NotNull Consumer task);

    @NotNull
    ScheduledTask runDelayed(@NotNull Plugin plugin, @NotNull Consumer task, long delay,
            @NotNull TimeUnit unit);

    @NotNull
    ScheduledTask runAtFixedRate(@NotNull Plugin plugin, @NotNull Consumer task,
            long initialDelay, long period, @NotNull TimeUnit unit);

    void cancelTasks(@NotNull Plugin plugin);
}
