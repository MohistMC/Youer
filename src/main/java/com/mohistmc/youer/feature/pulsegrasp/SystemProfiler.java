package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * System resource profiler — samples JVM heap usage and process CPU usage
 * every second, outputting a time series aligned with flowSeries to locate resource bottlenecks.
 */
public class SystemProfiler {

    private final List<SystemSample> samples = new ArrayList<>();
    private Thread updaterThread;
    private volatile boolean running = false;

    // ---- Lifecycle ----

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
        sample(); // capture the final segment
    }

    public void reset() {
        samples.clear();
    }

    // ---- Sampling ----

    private void sample() {
        Runtime runtime = Runtime.getRuntime();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long heapMax = runtime.maxMemory();
        samples.add(new SystemSample(System.currentTimeMillis() / 1000, heapUsed, heapMax, calcProcessCpuPercent()));
    }

    /** Get server process CPU usage (0-100), returns 0 when unavailable */
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

    // ---- JSON output ----

    JsonArray toJson() {
        JsonArray array = new JsonArray();
        for (SystemSample sample : samples) {
            JsonObject obj = new JsonObject();
            obj.addProperty("timestamp", sample.timestamp);
            obj.addProperty("heapUsedBytes", sample.heapUsedBytes);
            obj.addProperty("heapMaxBytes", sample.heapMaxBytes);
            obj.addProperty("heapUsagePercent", String.format("%.1f", sample.heapMaxBytes > 0 ? (double) sample.heapUsedBytes / sample.heapMaxBytes * 100 : 0));
            obj.addProperty("cpuPercent", String.format("%.1f", sample.cpuPercent));
            array.add(obj);
        }
        return array;
    }
}
