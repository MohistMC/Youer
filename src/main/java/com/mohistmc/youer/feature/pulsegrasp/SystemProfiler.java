package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统资源脉象分析器 — 每秒采样服务器 JVM 堆内存使用与进程 CPU 使用率，
 * 输出与网络流量（flowSeries）对齐的时间序列，用于对照性能波动定位资源瓶颈。
 */
public class SystemProfiler {

    private final List<SystemSample> samples = new ArrayList<>();
    private Thread updaterThread;
    private volatile boolean running = false;

    // ---- 生命周期 ----

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
        sample(); // 补录最后一段
    }

    public void reset() {
        samples.clear();
    }

    // ---- 采样 ----

    private void sample() {
        Runtime runtime = Runtime.getRuntime();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long heapMax = runtime.maxMemory();
        samples.add(new SystemSample(System.currentTimeMillis() / 1000, heapUsed, heapMax, calcProcessCpuPercent()));
    }

    /** 获取服务器进程 CPU 使用率（0-100），不可用时返回 0 */
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

    // ---- JSON 输出 ----

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
