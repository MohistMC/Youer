package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Paper Folia region scheduler API surface (stub JAR for plugin bytecode compatibility).
 */
public interface RegionScheduler {

    void execute(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Runnable run);

    default void execute(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable run) {
        this.execute(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, run);
    }

    @NotNull
    ScheduledTask run(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Consumer task);

    default @NotNull ScheduledTask run(@NotNull Plugin plugin, @NotNull Location location, @NotNull Consumer task) {
        return this.run(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task);
    }

    @NotNull
    ScheduledTask runDelayed(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Consumer task,
            long delayTicks);

    default @NotNull ScheduledTask runDelayed(@NotNull Plugin plugin, @NotNull Location location, @NotNull Consumer task,
            long delayTicks) {
        return this.runDelayed(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task, delayTicks);
    }

    @NotNull
    ScheduledTask runAtFixedRate(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Consumer task,
            long initialDelayTicks, long periodTicks);

    default @NotNull ScheduledTask runAtFixedRate(@NotNull Plugin plugin, @NotNull Location location, @NotNull Consumer task,
            long initialDelayTicks, long periodTicks) {
        return this.runAtFixedRate(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task, initialDelayTicks, periodTicks);
    }
}
