package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Paper Folia global region scheduler API surface (stub JAR for plugin bytecode compatibility).
 */
public interface GlobalRegionScheduler {

    void execute(@NotNull Plugin plugin, @NotNull Runnable run);

    @NotNull
    ScheduledTask run(@NotNull Plugin plugin, @NotNull Consumer task);

    @NotNull
    ScheduledTask runDelayed(@NotNull Plugin plugin, @NotNull Consumer task, long delayTicks);

    @NotNull
    ScheduledTask runAtFixedRate(@NotNull Plugin plugin, @NotNull Consumer task,
            long initialDelayTicks, long periodTicks);

    void cancelTasks(@NotNull Plugin plugin);
}
