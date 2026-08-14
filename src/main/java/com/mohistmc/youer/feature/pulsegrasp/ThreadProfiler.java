package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 线程 CPU 脉象分析器 — 在诊断停止时捕获所有线程的 CPU 时间、状态和堆栈信息，
 * 提供 JVM 线程级别的资源消耗画像，与 Tick 切脉和网络包数据融合输出。
 */
public class ThreadProfiler {

    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    static {
        threadMXBean.setThreadCpuTimeEnabled(true);
        if (threadMXBean.isThreadContentionMonitoringSupported()) {
            threadMXBean.setThreadContentionMonitoringEnabled(true);
        }
    }

    /** 捕获当前所有线程的 CPU 时间快照 */
    public JsonObject capture() {
        long[] ids = threadMXBean.getAllThreadIds();
        // 批量获取线程信息，减少逐线程 native 调用（profiler attach 期间 JVMTI 调用会被放大）
        ThreadInfo[] infos = threadMXBean.getThreadInfo(ids, 20);
        List<ThreadCpuTime> list = new ArrayList<>(ids.length);

        for (int i = 0; i < ids.length; i++) {
            long id = ids[i];
            ThreadInfo threadInfo = infos[i];
            if (threadInfo == null) continue;

            ThreadCpuTime item = new ThreadCpuTime();
            item.id = id;
            item.cpuTime = threadMXBean.getThreadCpuTime(id) / 1_000_000;
            item.userTime = threadMXBean.getThreadUserTime(id) / 1_000_000;
            item.name = threadInfo.getThreadName();
            item.state = threadInfo.getThreadState().toString();
            item.blockedTime = threadInfo.getBlockedTime();
            item.waitedTime = threadInfo.getWaitedTime();
            item.blockedCount = threadInfo.getBlockedCount();
            item.waitedCount = threadInfo.getWaitedCount();

            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : threadInfo.getStackTrace()) {
                stackTrace.append("  at ").append(element).append("\n");
            }
            item.stackTrace = stackTrace.toString();

            if (threadInfo.getLockInfo() != null) {
                item.lockInfo = threadInfo.getLockInfo().toString();
                item.lockOwnerId = threadInfo.getLockOwnerId();
                if (threadInfo.getLockOwnerId() != -1) {
                    ThreadInfo lockOwnerInfo = threadMXBean.getThreadInfo(threadInfo.getLockOwnerId());
                    if (lockOwnerInfo != null) {
                        item.lockOwnerName = lockOwnerInfo.getThreadName();
                    }
                }
            }

            list.add(item);
        }

        list.sort(Comparator.comparingLong(t -> -t.cpuTime));

        // 计算汇总
        long totalCpuTime = list.stream().mapToLong(t -> t.cpuTime).sum();
        long totalUserTime = list.stream().mapToLong(t -> t.userTime).sum();

        // 构建 JSON
        JsonObject root = new JsonObject();
        root.addProperty("totalThreads", list.size());
        root.addProperty("totalCpuTimeMs", totalCpuTime);
        root.addProperty("totalUserTimeMs", totalUserTime);

        JsonArray threads = new JsonArray();
        for (ThreadCpuTime t : list) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", t.id);
            obj.addProperty("name", t.name);
            obj.addProperty("state", t.state);
            obj.addProperty("cpuTimeMs", t.cpuTime);
            obj.addProperty("userTimeMs", t.userTime);
            obj.addProperty("cpuPercentage", totalCpuTime > 0 ? String.format("%.2f", (double) t.cpuTime / totalCpuTime * 100) : "0");
            obj.addProperty("blockedTime", t.blockedTime);
            obj.addProperty("waitedTime", t.waitedTime);
            obj.addProperty("blockedCount", t.blockedCount);
            obj.addProperty("waitedCount", t.waitedCount);
            // 无条件输出（空值时给空串，保证前端 schema 稳定）
            obj.addProperty("lockInfo", t.lockInfo != null ? t.lockInfo : "");
            obj.addProperty("lockOwnerId", t.lockOwnerId);
            obj.addProperty("lockOwnerName", t.lockOwnerName != null ? t.lockOwnerName : "");
            obj.addProperty("stackTrace", t.stackTrace);
            threads.add(obj);
        }
        root.add("threads", threads);

        return root;
    }

    // ---- 内部数据结构 ----

    private static class ThreadCpuTime {
        long id;
        long cpuTime;
        long userTime;
        String name;
        String state;
        long blockedTime;
        long waitedTime;
        long blockedCount;
        long waitedCount;
        String stackTrace;
        String lockInfo;
        long lockOwnerId = -1;
        String lockOwnerName;
    }
}