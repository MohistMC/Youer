package com.mohistmc.youer.region;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

// Background flusher for the Linear region format. Writes/clears mark a LinearRegionFile dirty and
// enqueue it here instead of blocking the calling IOWorker thread with compression or disk I/O.
// PENDING coalesces repeated writes and IN_FLIGHT guarantees that only one immutable snapshot of a
// particular region is written at once. The fixed worker pool still allows different regions to be
// processed in parallel.
public final class LinearRegionFileFlusher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object LIFECYCLE_LOCK = new Object();

    private static final Set<LinearRegionFile> PENDING = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<LinearRegionFile> IN_FLIGHT = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<LinearRegionFile> KNOWN = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static ScheduledExecutorService scheduler;
    private static ExecutorService workers;
    private static boolean shuttingDown;

    private LinearRegionFileFlusher() {}

    public static void scheduleFlush(LinearRegionFile regionFile) {
        KNOWN.add(regionFile);
        boolean flushDuringShutdown;
        synchronized (LIFECYCLE_LOCK) {
            flushDuringShutdown = shuttingDown;
            if (!flushDuringShutdown) {
                ensureStartedLocked();
            }
        }
        PENDING.add(regionFile);
        if (flushDuringShutdown) {
            try {
                flushAndWait(regionFile, regionFile.currentGeneration());
            } catch (IOException ioexception) {
                LOGGER.error("Failed to flush linear region file {} during shutdown", regionFile.getPath(), ioexception);
            }
        }
    }

    public static void flushAndWait(LinearRegionFile regionFile, long targetGeneration) throws IOException {
        if (regionFile.isPersisted(targetGeneration)) {
            return;
        }

        KNOWN.add(regionFile);
        final boolean flushDirectly;
        synchronized (LIFECYCLE_LOCK) {
            if (workers == null || workers.isShutdown()) {
                if (shuttingDown) {
                    flushDirectly = true;
                } else {
                    ensureStartedLocked();
                    flushDirectly = false;
                }
            } else {
                flushDirectly = false;
            }
        }
        if (flushDirectly) {
            flushDirect(regionFile, targetGeneration);
            return;
        }

        PENDING.add(regionFile);
        while (!regionFile.isPersisted(targetGeneration)) {
            long observedAttempt = regionFile.completedFlushAttempts();
            if (!submit(regionFile)) {
                // The executor may have entered shutdown after the lifecycle check. The direct
                // path uses the same IN_FLIGHT slot, so it cannot race an already-running write.
                flushDirect(regionFile, targetGeneration);
                return;
            }
            final IOException failure;
            try {
                failure = regionFile.awaitFlushProgress(targetGeneration, observedAttempt);
            } catch (InterruptedException interruptedexception) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while flushing linear region file " + regionFile.getPath(), interruptedexception);
            }
            if (failure != null) {
                throw failure;
            }
        }
    }

    public static void forget(LinearRegionFile regionFile) {
        PENDING.remove(regionFile);
        KNOWN.remove(regionFile);
    }

    private static void ensureStartedLocked() {
        if (scheduler != null) {
            return;
        }
        int threads = Math.max(1, io.papermc.paper.configuration.GlobalConfiguration.get().unsupportedSettings.linearFlushThreads);
        int intervalSeconds = Math.max(1, io.papermc.paper.configuration.GlobalConfiguration.get().unsupportedSettings.linearFlushIntervalSeconds);

        AtomicInteger workerId = new AtomicInteger();
        workers = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "Linear Region Flusher #" + workerId.getAndIncrement());
            thread.setDaemon(false); // must not be daemon: killing mid-write/mid-rename can corrupt a region file
            thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Uncaught exception in {}", t.getName(), e));
            return thread;
        });

        ScheduledExecutorService newScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Linear Region Flusher Scheduler");
            thread.setDaemon(false);
            thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Uncaught exception in {}", t.getName(), e));
            return thread;
        });
        newScheduler.scheduleAtFixedRate(LinearRegionFileFlusher::drain, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        scheduler = newScheduler;
    }

    private static void drain() {
        if (PENDING.isEmpty()) {
            return;
        }

        Iterator<LinearRegionFile> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            LinearRegionFile regionFile = iterator.next();
            submit(regionFile);
        }
    }

    private static boolean submit(LinearRegionFile regionFile) {
        if (!IN_FLIGHT.add(regionFile)) {
            return true;
        }

        final ExecutorService pool;
        synchronized (LIFECYCLE_LOCK) {
            pool = workers;
        }
        if (pool == null || pool.isShutdown()) {
            IN_FLIGHT.remove(regionFile);
            return false;
        }

        PENDING.remove(regionFile);
        try {
            pool.execute(() -> flushOne(regionFile));
            return true;
        } catch (RejectedExecutionException rejectedexecutionexception) {
            IN_FLIGHT.remove(regionFile);
            PENDING.add(regionFile);
            return false;
        }
    }

    private static void flushOne(LinearRegionFile regionFile) {
        LinearRegionFile.FlushSnapshot snapshot = regionFile.createFlushSnapshot();
        if (snapshot == null) {
            IN_FLIGHT.remove(regionFile);
            return;
        }

        IOException failure = null;
        try {
            regionFile.writeSnapshot(snapshot);
        } catch (IOException ioexception) {
            failure = ioexception;
            LOGGER.error("Failed to background-flush linear region file {}", regionFile.getPath(), ioexception);
        } catch (RuntimeException runtimeexception) {
            failure = new IOException("Failed to flush linear region file " + regionFile.getPath(), runtimeexception);
            LOGGER.error("Failed to background-flush linear region file {}", regionFile.getPath(), runtimeexception);
        } finally {
            // Release the per-region execution slot before waking explicit flush waiters. A waiter
            // can then immediately submit the next generation instead of waiting for the timer.
            IN_FLIGHT.remove(regionFile);
            if (regionFile.completeFlush(snapshot, failure)) {
                PENDING.add(regionFile);
            }
        }
    }

    private static void flushDirect(LinearRegionFile regionFile, long targetGeneration) throws IOException {
        while (!regionFile.isPersisted(targetGeneration)) {
            long observedAttempt = regionFile.completedFlushAttempts();
            if (!IN_FLIGHT.add(regionFile)) {
                final IOException failure;
                try {
                    failure = regionFile.awaitFlushProgress(targetGeneration, observedAttempt);
                } catch (InterruptedException interruptedexception) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while flushing linear region file " + regionFile.getPath(), interruptedexception);
                }
                if (failure != null) {
                    throw failure;
                }
                continue;
            }

            LinearRegionFile.FlushSnapshot snapshot = regionFile.createFlushSnapshot();
            if (snapshot == null) {
                IN_FLIGHT.remove(regionFile);
                return;
            }
            IOException failure = null;
            try {
                regionFile.writeSnapshot(snapshot);
            } catch (IOException ioexception) {
                failure = ioexception;
            } catch (RuntimeException runtimeexception) {
                failure = new IOException("Failed to flush linear region file " + regionFile.getPath(), runtimeexception);
            } finally {
                IN_FLIGHT.remove(regionFile);
                if (regionFile.completeFlush(snapshot, failure)) {
                    PENDING.add(regionFile);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }

    // Drains any remaining dirty regions and waits for in-flight flushes before shutdown, so the JVM
    // never exits (and no thread is killed) mid-compress/mid-rename of a region file.
    public static void shutdown() {
        ScheduledExecutorService currentScheduler;
        ExecutorService currentWorkers;
        synchronized (LIFECYCLE_LOCK) {
            shuttingDown = true;
            currentScheduler = scheduler;
            currentWorkers = workers;
        }

        if (currentScheduler != null) {
            currentScheduler.shutdown();
        }

        for (LinearRegionFile regionFile : KNOWN.toArray(LinearRegionFile[]::new)) {
            try {
                flushAndWait(regionFile, regionFile.currentGeneration());
            } catch (IOException ioexception) {
                LOGGER.error("Failed to flush linear region file {} during shutdown", regionFile.getPath(), ioexception);
            }
        }

        if (currentWorkers == null) {
            return;
        }
        currentWorkers.shutdown();
        try {
            if (!currentWorkers.awaitTermination(60, TimeUnit.SECONDS)) {
                LOGGER.error("Linear region flusher did not shut down in time; some pending writes may not have been saved");
            }
        } catch (InterruptedException interruptedexception) {
            Thread.currentThread().interrupt();
        }

        synchronized (LIFECYCLE_LOCK) {
            scheduler = null;
            workers = null;
        }
    }
}
