package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Paper-compatible API surface shipped as a separate library JAR (not inside minecraft/neoforge modules)
 * so plugins whose bytecode references Folia scheduler types can load without JPMS split-package errors.
 */
public interface ScheduledTask {

    @NotNull
    Plugin getOwningPlugin();

    boolean isRepeatingTask();

    @NotNull
    CancelledState cancel();

    @NotNull
    ExecutionState getExecutionState();

    default boolean isCancelled() {
        ExecutionState state = this.getExecutionState();
        return state == ExecutionState.CANCELLED || state == ExecutionState.CANCELLED_RUNNING;
    }

    enum CancelledState {
        CANCELLED_BY_CALLER,
        CANCELLED_ALREADY,
        RUNNING,
        ALREADY_EXECUTED,
        NEXT_RUNS_CANCELLED,
        NEXT_RUNS_CANCELLED_ALREADY,
    }

    enum ExecutionState {
        IDLE,
        RUNNING,
        FINISHED,
        CANCELLED,
        CANCELLED_RUNNING,
    }
}
