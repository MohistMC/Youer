package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Samples heap, process CPU and GC once per second. GC pause time is tracked
 * separately because it can cause MSPT spikes that no tick instrumentation explains.
 */
public class SystemProfiler {

    private final List<SystemSample> samples = new ArrayList<>();
    private Thread updaterThread;
    private volatile boolean running = false;

    // GC counters, cumulative
    private long lastGcCount;
    private long lastGcTimeMs;
    private long totalGcCount;
    private long totalGcTimeMs;
    private long maxGcPauseMs;

    void start() {
        if (running) return;
        running = true;
        reset();
        updaterThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                    sample();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "PulseGrasp-System-Updater");
        updaterThread.setDaemon(true);
        updaterThread.start();
    }

    void stop() {
        running = false;
        if (updaterThread != null) {
            updaterThread.interrupt();
            updaterThread = null;
        }
        sample(); // final segment
    }

    public void reset() {
        samples.clear();
        lastGcCount = currentGcCount();
        lastGcTimeMs = currentGcTimeMs();
        totalGcCount = 0;
        totalGcTimeMs = 0;
        maxGcPauseMs = 0;
    }

    private void sample() {
        Runtime runtime = Runtime.getRuntime();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long heapMax = runtime.maxMemory();

        long gcCount = currentGcCount();
        long gcTime = currentGcTimeMs();
        long gcCountDelta = Math.max(0, gcCount - lastGcCount);
        long gcTimeDelta = Math.max(0, gcTime - lastGcTimeMs);
        lastGcCount = gcCount;
        lastGcTimeMs = gcTime;
        totalGcCount += gcCountDelta;
        totalGcTimeMs += gcTimeDelta;
        if (gcTimeDelta > maxGcPauseMs) maxGcPauseMs = gcTimeDelta;

        samples.add(new SystemSample(System.currentTimeMillis() / 1000, heapUsed, heapMax,
                calcProcessCpuPercent(), gcCountDelta, gcTimeDelta));
    }

    public long getTotalGcCount() {
        return totalGcCount;
    }

    public long getTotalGcTimeMs() {
        return totalGcTimeMs;
    }

    /** Largest GC pause seen within a single one-second window. */
    public long getMaxGcPauseMs() {
        return maxGcPauseMs;
    }

    private static long currentGcCount() {
        long total = 0;
        try {
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                long c = gc.getCollectionCount();
                if (c > 0) total += c;
            }
        } catch (Throwable ignored) {
        }
        return total;
    }

    private static long currentGcTimeMs() {
        long total = 0;
        try {
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                long t = gc.getCollectionTime();
                if (t > 0) total += t;
            }
        } catch (Throwable ignored) {
        }
        return total;
    }

    private double calcProcessCpuPercent() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double load = osBean.getProcessCpuLoad();
            return load < 0 ? 0 : load * 100;
        } catch (Exception e) {
            return 0;
        }
    }

    JsonArray toJson() {
        JsonArray array = new JsonArray();
        for (SystemSample sample : samples) {
            JsonObject obj = new JsonObject();
            obj.addProperty("timestamp", sample.timestamp);
            obj.addProperty("heapUsedBytes", sample.heapUsedBytes);
            obj.addProperty("heapMaxBytes", sample.heapMaxBytes);
            obj.addProperty("heapUsagePercent", String.format("%.1f", sample.heapMaxBytes > 0 ? (double) sample.heapUsedBytes / sample.heapMaxBytes * 100 : 0));
            obj.addProperty("cpuPercent", String.format("%.1f", sample.cpuPercent));
            obj.addProperty("gcCount", sample.gcCount);
            obj.addProperty("gcTimeMs", sample.gcTimeMs);
            array.add(obj);
        }
        return array;
    }
}
