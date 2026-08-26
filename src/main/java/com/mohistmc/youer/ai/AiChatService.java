package com.mohistmc.youer.ai;

import com.mohistmc.youer.ai.history.AiConversationSnapshot;
import com.mohistmc.youer.ai.history.AiConversationStore;
import com.mohistmc.youer.ai.model.AiChatRequest;
import com.mohistmc.youer.ai.model.AiChatResponse;
import com.mohistmc.youer.ai.model.AiMessage;
import com.mohistmc.youer.ai.model.AiRole;
import com.mohistmc.youer.ai.provider.AiProvider;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class AiChatService implements AutoCloseable {

    private final AtomicReference<AiRuntime> runtime;
    private final AiConversationStore history;
    private final ConcurrentLinkedQueue<ThreadPoolExecutor> executors = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<?>> playerChains = new ConcurrentHashMap<>();
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final AtomicLong acceptedRequests = new AtomicLong();
    private volatile ThreadPoolExecutor executor;

    public AiChatService(AiRuntime runtime, AiConversationStore history) {
        this.runtime = new AtomicReference<>(runtime);
        this.history = history;
        this.executor = createExecutor(runtime);
        this.executors.add(executor);
    }

    public CompletableFuture<AiChatResponse> chat(UUID playerId, String message) {
        if (!accepting.get()) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("AI chat service is retired"));
        }
        AiRuntime snapshot = runtime.get();
        if (!snapshot.enabled() || snapshot.defaultProfile() == null || snapshot.defaultProvider() == null) {
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
                    ignoredValue -> submit(() -> invoke(snapshot, playerId, conversationVersion, message)));
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

    public synchronized void replaceRuntime(AiRuntime replacement) {
        AiRuntime previous = runtime.get();
        if (previous.workerThreads() != replacement.workerThreads()
                || previous.queueCapacity() != replacement.queueCapacity()) {
            ThreadPoolExecutor previousExecutor = executor;
            ThreadPoolExecutor replacementExecutor = createExecutor(replacement);
            executors.add(replacementExecutor);
            executor = replacementExecutor;
            previousExecutor.shutdown();
        }
        runtime.set(replacement);
    }

    public AiRuntime runtime() {
        return runtime.get();
    }

    public AiConversationSnapshot history(UUID playerId) {
        return history.snapshot(playerId);
    }

    public Map<UUID, AiConversationSnapshot> histories() {
        return history.snapshots();
    }

    public int historySize(UUID playerId) {
        return history.size(playerId);
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
            for (ThreadPoolExecutor current : executors) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0 || !current.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
                    shutdownNow();
                    return;
                }
            }
        } catch (InterruptedException exception) {
            shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private AiChatResponse invoke(AiRuntime snapshot, UUID playerId, long version, String message) {
        AiProvider provider = snapshot.defaultProvider();
        ArrayList<AiMessage> messages = new ArrayList<>(history.snapshot(playerId).messages());
        AiMessage userMessage = new AiMessage(AiRole.USER, message);
        messages.add(userMessage);
        AiChatResponse response = provider.chat(new AiChatRequest(messages));
        history.appendIfVersion(
                playerId,
                version,
                userMessage,
                new AiMessage(AiRole.ASSISTANT, response.content()),
                snapshot.maxHistory());
        return response;
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
        ThreadPoolExecutor selected = executor;
        try {
            selected.execute(command);
        } catch (RejectedExecutionException exception) {
            ThreadPoolExecutor replacement = executor;
            if (selected != replacement && accepting.get()) {
                try {
                    replacement.execute(command);
                } catch (RejectedExecutionException retryFailure) {
                    result.completeExceptionally(retryFailure);
                }
            } else {
                result.completeExceptionally(exception);
            }
        }
        return result;
    }

    private void shutdownWhenRetiredAndIdle() {
        if (!accepting.get() && playerChains.isEmpty()) {
            executors.forEach(ThreadPoolExecutor::shutdown);
        }
    }

    private void shutdownNow() {
        executors.forEach(ThreadPoolExecutor::shutdownNow);
    }

    private static ThreadPoolExecutor createExecutor(AiRuntime runtime) {
        return new ThreadPoolExecutor(
                runtime.workerThreads(),
                runtime.workerThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(runtime.queueCapacity()),
                new AiThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    private static final class AiThreadFactory implements ThreadFactory {
        private static final AtomicInteger SEQUENCE = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Youer AI Worker-" + SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
