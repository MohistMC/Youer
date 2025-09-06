package com.mohistmc.youer.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mohistmc.youer.Youer;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.CommandSender;

public class YouerThreadCost {
    static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    static {
        threadMXBean.setThreadCpuTimeEnabled(true);
        if (threadMXBean.isThreadContentionMonitoringSupported()) {
            threadMXBean.setThreadContentionMonitoringEnabled(true);
        }
    }

    public static void dumpThreadCpuTime(CommandSender sender) {
        try {
            List<ThreadCpuTime> list = new ArrayList<>();
            long[] ids = threadMXBean.getAllThreadIds();

            for (long id : ids) {
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(id, 20);
                if (threadInfo != null) {
                    ThreadCpuTime item = new ThreadCpuTime();
                    item.cpuTime = threadMXBean.getThreadCpuTime(id) / 1000000;
                    item.userTime = threadMXBean.getThreadUserTime(id) / 1000000;
                    item.name = threadInfo.getThreadName();
                    item.id = id;
                    item.state = threadInfo.getThreadState().toString();
                    item.blockedTime = threadInfo.getBlockedTime();
                    item.waitedTime = threadInfo.getWaitedTime();
                    item.blockedCount = threadInfo.getBlockedCount();
                    item.waitedCount = threadInfo.getWaitedCount();

                    StringBuilder stackTrace = new StringBuilder();
                    for (StackTraceElement element : threadInfo.getStackTrace()) {
                        stackTrace.append("  at ").append(element.toString()).append("\n");
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
            }

            list.sort(Comparator.comparingLong(i -> -i.cpuTime));

            ThreadCostReport report = new ThreadCostReport();
            report.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            report.threads = list;
            report.totalThreads = list.size();

            long totalCpuTime = list.stream().mapToLong(t -> t.cpuTime).sum();
            long totalUserTime = list.stream().mapToLong(t -> t.userTime).sum();
            report.totalCpuTime = totalCpuTime;
            report.totalUserTime = totalUserTime;

            Path exportDir = Paths.get("thread-dumps");
            Files.createDirectories(exportDir);

            String fileName = "thread-cost-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + ".json";
            Path filePath = exportDir.resolve(fileName);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                gson.toJson(report, writer);
            }

            sender.sendMessage(Youer.i18n.as("youer.thread.cost.saved", filePath.toAbsolutePath().toString()));

        } catch (IOException e) {
            sender.sendMessage(Youer.i18n.as("youer.thread.cost.save.failed", e.getMessage()));
        } catch (Exception e) {
            Youer.LOGGER.error(Youer.i18n.as("youer.thread.cost.error"), e);
        }
    }

    @Setter
    @Getter
    public static class ThreadCpuTime {
        private long id;
        private long cpuTime;
        private long userTime;
        private String name;
        private String state;
        private long blockedTime;
        private long waitedTime;
        private long blockedCount;
        private long waitedCount;
        private String stackTrace;
        private String lockInfo;
        private long lockOwnerId = -1;
        private String lockOwnerName;

    }

    @Setter
    @Getter
    public static class ThreadCostReport {
        private String timestamp;
        private int totalThreads;
        private long totalCpuTime;
        private long totalUserTime;
        private List<ThreadCpuTime> threads;

    }
}
