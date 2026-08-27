package com.mohistmc.youer.ai;

import com.mohistmc.youer.ai.history.AiConversationSnapshot;
import com.mohistmc.youer.ai.history.AiConversationStore;
import com.mohistmc.youer.ai.model.AiChatRequest;
import com.mohistmc.youer.ai.model.AiChatResponse;
import com.mohistmc.youer.ai.model.AiMessage;
import com.mohistmc.youer.ai.model.AiRole;
import com.mohistmc.youer.ai.provider.AiProvider;
import com.mohistmc.youer.ai.skill.AiCapabilitySnapshot;
import com.mohistmc.youer.ai.skill.AiCapabilitySnapshotProvider;
import com.mohistmc.youer.ai.tool.AiAgentLoop;
import com.mohistmc.youer.ai.tool.AiAgentRequest;
import com.mohistmc.youer.ai.tool.AiToolRegistry;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class AiChatService implements AutoCloseable {

    private final AiRuntime runtime;
    private final AiConversationStore history;
    private final ConcurrentHashMap<UUID, CompletableFuture<?>> playerChains = new ConcurrentHashMap<>();
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final AtomicLong acceptedRequests = new AtomicLong();
    private final ThreadPoolExecutor executor;
    private final AiAgentLoop agentLoop;
    private final AiCapabilitySnapshotProvider capabilitySnapshots;

    public AiChatService(AiRuntime runtime, AiConversationStore history) {
        this(runtime, history, null, null);
    }

    public AiChatService(
            AiRuntime runtime,
            AiConversationStore history,
            AiAgentLoop agentLoop,
            AiCapabilitySnapshotProvider capabilitySnapshots) {
        this.runtime = runtime;
        this.history = history;
        this.agentLoop = agentLoop;
        this.capabilitySnapshots = capabilitySnapshots;
        if (agentLoop != null && capabilitySnapshots == null) {
            throw new IllegalArgumentException("Agent loop requires capability snapshots");
        }
        this.executor = createExecutor(runtime);
    }

    public CompletableFuture<AiChatResponse> chat(AiToolContext context, String message) {
        UUID playerId = context.playerId();
        if (!accepting.get()) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("AI chat service is retired"));
        }
        AiRuntime snapshot = runtime;
        if (!snapshot.enabled() || snapshot.profile() == null || snapshot.provider() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("AI chat service is unavailable"));
        }
        long capacity = (long) snapshot.workerThreads() + snapshot.queueCapacity();
        if (acceptedRequests.incrementAndGet() > capacity) {
            acceptedRequests.decrementAndGet();
            return CompletableFuture.failedFuture(new RejectedExecutionException("AI chat service is busy"));
        }
        long conversationVersion = history.snapshot(playerId).version();
        AtomicReference<CompletableFuture<AiChatResponse>> created = new AtomicReference<>();
        playerChains.compute(playerId, (ignored, previous) -> {
            CompletableFuture<?> gate = previous == null
                    ? CompletableFuture.completedFuture(null)
                    : previous.handle((value, failure) -> null);
            CompletableFuture<AiChatResponse> next = gate.thenCompose(
                    ignoredValue -> submitStage(() -> invoke(
                            snapshot, context, conversationVersion, message)));
            created.set(next);
            return next;
        });

        CompletableFuture<AiChatResponse> result = created.get();
        result.whenComplete((value, failure) -> {
            acceptedRequests.decrementAndGet();
            playerChains.remove(playerId, result);
            shutdownWhenRetiredAndIdle();
        });
        return result;
    }

    public AiRuntime runtime() {
        return runtime;
    }

    public Map<UUID, AiConversationSnapshot> histories() {
        return history.snapshots();
    }

    public void clear(UUID playerId) {
        history.clear(playerId);
    }

    public void clearAll() {
        history.clearAll();
    }

    public void retire() {
        accepting.set(false);
        shutdownWhenRetiredAndIdle();
    }

    @Override
    public void close() {
        retire();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        try {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0 || !executor.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private java.util.concurrent.CompletionStage<AiChatResponse> invoke(
            AiRuntime snapshot, AiToolContext context, long version, String message) {
        UUID playerId = context.playerId();
        ArrayList<AiMessage> messages = new ArrayList<>(history.snapshot(playerId).messages());
        AiMessage userMessage = new AiMessage(AiRole.USER, message);
        messages.add(userMessage);
        if (capabilitySnapshots != null) {
            return capabilitySnapshots.snapshot(context, snapshot.toolsEnabled())
                    .thenCompose(capabilities -> invokeWithCapabilities(
                            snapshot, context, version, userMessage, messages, capabilities));
        }
        return invokeWithCapabilities(snapshot, context, version, userMessage, messages, null);
    }

    private java.util.concurrent.CompletionStage<AiChatResponse> invokeWithCapabilities(
            AiRuntime snapshot,
            AiToolContext context,
            long version,
            AiMessage userMessage,
            ArrayList<AiMessage> messages,
            AiCapabilitySnapshot capabilities) {
        UUID playerId = context.playerId();
        AiProvider provider = snapshot.provider();
        if (capabilities != null && !capabilities.systemContext().isBlank()) {
            messages.addFirst(new AiMessage(AiRole.SYSTEM, capabilities.systemContext()));
        }
        if (agentLoop == null) {
            return submitStage(() -> provider.chat(new AiChatRequest(messages)))
                    .thenApply(response -> {
                        history.appendIfVersion(playerId, version, userMessage,
                                new AiMessage(AiRole.ASSISTANT, response.content()), snapshot.maxHistory());
                        return response;
                    });
        }
        AiToolRegistry.Snapshot tools = java.util.Objects.requireNonNull(capabilities).tools();
        return agentLoop.run(new AiAgentRequest(provider, messages, tools, context,
                        snapshot.maxToolSteps(), snapshot.maxToolCallsPerTurn()), executor)
                .thenApply(result -> {
                    history.appendIfVersion(playerId, version, result.turn(), snapshot.maxHistory());
                    return result.response();
                });
    }

    private <T> CompletableFuture<T> submit(Supplier<T> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        Runnable command = () -> {
            try {
                result.complete(action.get());
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        };
        try {
            executor.execute(command);
        } catch (RejectedExecutionException exception) {
            result.completeExceptionally(exception);
        }
        return result;
    }

    private <T> CompletableFuture<T> submitStage(
            Supplier<? extends java.util.concurrent.CompletionStage<T>> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFuture<Void> scheduled = submit(() -> {
            try {
                action.get().whenComplete((value, failure) -> {
                    if (failure == null) result.complete(value);
                    else result.completeExceptionally(failure);
                });
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
            return null;
        });
        scheduled.whenComplete((ignored, failure) -> {
            if (failure != null) {
                result.completeExceptionally(failure);
            }
        });
        return result;
    }

    private void shutdownWhenRetiredAndIdle() {
        if (!accepting.get() && playerChains.isEmpty()) {
            executor.shutdown();
        }
    }

    private static ThreadPoolExecutor createExecutor(AiRuntime runtime) {
        return new ThreadPoolExecutor(
                runtime.workerThreads(),
                runtime.workerThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(runtime.workerThreads() + runtime.queueCapacity()),
                new AiThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    private static final class AiThreadFactory implements ThreadFactory {
        private static final AtomicInteger SEQUENCE = new AtomicInteger();

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread thread = new Thread(runnable, "Youer AI Worker-" + SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
