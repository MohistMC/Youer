package com.mohistmc.youer.ai.tool;

import com.mohistmc.youer.api.ai.tool.AiToolExecutionMode;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class AiExecutionDispatcher {
    private final Executor asyncExecutor;
    private final Executor mainThreadExecutor;

    public AiExecutionDispatcher(Executor asyncExecutor, Executor mainThreadExecutor) {
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor");
        this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
    }

    public <T> CompletionStage<T> dispatch(
            AiToolExecutionMode mode, Supplier<? extends CompletionStage<T>> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        Executor selected = mode == AiToolExecutionMode.MAIN_THREAD ? mainThreadExecutor : asyncExecutor;
        selected.execute(() -> {
            try {
                action.get().whenComplete((value, failure) -> {
                    if (failure == null) result.complete(value);
                    else result.completeExceptionally(failure);
                });
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        return result;
    }
}
