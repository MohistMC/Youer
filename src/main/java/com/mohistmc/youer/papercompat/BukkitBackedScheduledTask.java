package com.mohistmc.youer.papercompat;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public final class BukkitBackedScheduledTask implements ScheduledTask {

    private final Plugin owningPlugin;
    private final boolean repeating;
    private volatile BukkitTask bukkitTask;
    private volatile boolean oneShotFinished;

    public BukkitBackedScheduledTask(@NotNull Plugin owningPlugin, boolean repeating) {
        this.owningPlugin = owningPlugin;
        this.repeating = repeating;
    }

    public void setBukkitTask(@NotNull BukkitTask bukkitTask) {
        this.bukkitTask = bukkitTask;
    }

    public void markOneShotFinished() {
        this.oneShotFinished = true;
    }

    @Override
    public @NotNull Plugin getOwningPlugin() {
        return owningPlugin;
    }

    @Override
    public boolean isRepeatingTask() {
        return repeating;
    }

    @Override
    public @NotNull CancelledState cancel() {
        BukkitTask t = this.bukkitTask;
        if (t == null) {
            return CancelledState.ALREADY_EXECUTED;
        }
        if (t.isCancelled()) {
            return CancelledState.CANCELLED_ALREADY;
        }
        t.cancel();
        return repeating ? CancelledState.NEXT_RUNS_CANCELLED : CancelledState.CANCELLED_BY_CALLER;
    }

    @Override
    public @NotNull ExecutionState getExecutionState() {
        if (!repeating && oneShotFinished) {
            return ExecutionState.FINISHED;
        }
        BukkitTask t = this.bukkitTask;
        if (t != null && t.isCancelled()) {
            return repeating ? ExecutionState.CANCELLED_RUNNING : ExecutionState.CANCELLED;
        }
        return ExecutionState.IDLE;
    }
}
