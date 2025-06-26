package ca.spottedleaf.moonrise.common.util;

import ca.spottedleaf.concurrentutil.executor.standard.PrioritisedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MoonriseCommon {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoonriseCommon.class);

    // Paper start
    public static PrioritisedThreadPool WORKER_POOL;
    public static int WORKER_THREADS;
    public static void init(io.papermc.paper.configuration.GlobalConfiguration.ChunkSystem chunkSystem) {
        // Paper end
        int defaultWorkerThreads = Runtime.getRuntime().availableProcessors() / 2;
        if (defaultWorkerThreads <= 4) {
            defaultWorkerThreads = defaultWorkerThreads <= 3 ? 1 : 2;
        } else {
            defaultWorkerThreads = defaultWorkerThreads / 2;
        }
        defaultWorkerThreads = Integer.getInteger("Paper.WorkerThreadCount", Integer.valueOf(defaultWorkerThreads)); // Paper

        int workerThreads = chunkSystem.workerThreads; // Paper

        if (workerThreads <= 0) {
            workerThreads = defaultWorkerThreads;
        }

        WORKER_POOL = new PrioritisedThreadPool(
                "Paper Worker Pool", workerThreads, // Paper
                (final Thread thread, final Integer id) -> {
                    thread.setName("Paper Common Worker #" + id.intValue()); // Paper
                    thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(final Thread thread, final Throwable throwable) {
                            LOGGER.error("Uncaught exception in thread " + thread.getName(), throwable);
                        }
                    });
                }, (long)(20.0e6)); // 20ms
        WORKER_THREADS = workerThreads;
    }

    private MoonriseCommon() {}
}
