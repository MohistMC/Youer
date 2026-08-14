package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mohistmc.youer.util.I18n;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.bukkit.Bukkit;

/**
 * PulseGrasp — 服务端"把脉"诊断系统
 * <p>
 * 实时采集服务器每 tick 各环节耗时、实体类型耗时、方块实体类型耗时、
 * TPS/MSPT/Ping 生命体征时间序列，以及网络数据包发送统计，
 * 停止时输出融合 JSON 诊断报告，精准定位 MSPT 消耗源头与网络流量画像。
 * <p>
 * 融合数据特点：
 * - 方块实体同时有 tick 耗时（计算性能）和网络包字节（网络负载），可交叉分析
 * - 玩家维度同时有 Ping（延迟）和网络包字节（流量消耗），可定位网络卡顿用户
 * - 区块维度有网络包发送量，可发现异常高频区块
 */
public class PulseGrasp {

    private static final PulseGrasp instance = new PulseGrasp();
    private volatile boolean grasping = false;
    private long graspStartMs;

    // 记录者（start/stop 命令执行者）
    private String startName;
    private String startUuid;
    private String stopName;
    private String stopUuid;

    private final TickProfiler tickProfiler = new TickProfiler();
    private final PacketProfiler packetProfiler = new PacketProfiler();
    private final ThreadProfiler threadProfiler = new ThreadProfiler();
    private final SystemProfiler systemProfiler = new SystemProfiler();

    // 异步诊断线程（报告生成在后台执行，避免阻塞 Server thread）
    private Thread diagnoseThread;
    private String lastReportPath;
    private String notifyUuid;

    // ---- 单例 ----

    public static PulseGrasp instance() {
        return instance;
    }

    public static boolean isGrasping() {
        return instance.grasping;
    }

    // ---- 生命周期 ----

    /** 开始把脉 — 同时启动 Tick 切脉、网络数据包分析和系统资源采样 */
    public void startGrasp(String name, String uuid) {
        if (grasping) return;
        joinDiagnoseThread(); // 等待上一次诊断线程结束，防止其读取到被 reset 的数据
        grasping = true;
        graspStartMs = System.currentTimeMillis();
        startName = name;
        startUuid = uuid;
        stopName = null;
        stopUuid = null;
        notifyUuid = null;
        tickProfiler.reset();
        packetProfiler.reset();
        packetProfiler.start();
        systemProfiler.reset();
        systemProfiler.start();
    }

    /** 停止把脉 — 报告生成挪到后台线程异步执行，Server thread 立即返回 */
    public void stopGraspAndDiagnose(String name, String uuid) {
        if (!grasping) return;
        stopName = name;
        stopUuid = uuid;
        notifyUuid = uuid;
        grasping = false;
        packetProfiler.stop();
        systemProfiler.stop();
        diagnoseThread = new Thread(() -> {
            diagnose();
            notifyReportDone();
        }, "PulseGrasp-Diagnose");
        diagnoseThread.setDaemon(true); // 服务器关停时不阻塞退出
        diagnoseThread.start();
    }

    private void joinDiagnoseThread() {
        if (diagnoseThread != null && diagnoseThread.isAlive()) {
            try {
                diagnoseThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** 报告生成完成后的通知 — 控制台日志 + 主线程通知发起玩家 */
    private void notifyReportDone() {
        Bukkit.getLogger().info(I18n.as("pulsegrasp.done.log", lastReportPath));
        if (notifyUuid == null || "none".equals(notifyUuid)) return;
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (server == null) return;
        server.execute(() -> {
            try {
                org.bukkit.entity.Player player = Bukkit.getPlayer(java.util.UUID.fromString(notifyUuid));
                if (player != null && player.isOnline()) {
                    player.sendMessage(I18n.as("pulsegrasp.done", lastReportPath));
                }
            } catch (IllegalArgumentException ignored) {
                // UUID 非法时仅保留控制台日志
            }
        });
    }

    // ============ 静态便捷方法 ============

    public static void start() {
        instance.startGrasp("console", "none");
    }

    public static void start(String name, String uuid) {
        instance.startGrasp(name, uuid);
    }

    public static void stop() {
        instance.stopGraspAndDiagnose("console", "none");
    }

    public static void stop(String name, String uuid) {
        instance.stopGraspAndDiagnose(name, uuid);
    }

    public static void feelPulse(String meridian) {
        if (instance.grasping) instance.tickProfiler.feelPulse(meridian);
    }

    public static void setLevel(String dimension) {
        if (instance.grasping) instance.tickProfiler.setLevel(dimension);
    }

    public static void pulseComplete() {
        if (instance.grasping) instance.tickProfiler.pulseComplete();
    }

    public static void markTick() {
        if (instance.grasping) instance.tickProfiler.markTick();
    }

    public static void recordEntityPulse(String entityType, long nanos, java.util.UUID uuid, String world, int x, int y, int z) {
        if (instance.grasping) instance.tickProfiler.recordEntityPulse(entityType, nanos, uuid, world, x, y, z);
    }

    public static void recordBlockEntityPulse(String blockEntityType, long nanos, String world, int x, int y, int z) {
        if (instance.grasping) instance.tickProfiler.recordBlockEntityPulse(blockEntityType, nanos, world, x, y, z);
    }

    public static void recordChunkStats(int totalChunks, int activeChunks) {
        if (instance.grasping) instance.tickProfiler.recordChunkStat(totalChunks, activeChunks);
    }

    public static int getTickCount() {
        return instance.tickProfiler.getTickCount();
    }

    // ---- 内部委托（不对外暴露 instance()） ----

    public TickProfiler tickProfiler() {
        return tickProfiler;
    }

    public PacketProfiler packetProfiler() {
        return packetProfiler;
    }

    // ---- 诊断报告 ----

    private void diagnose() {
        long durationMs = System.currentTimeMillis() - graspStartMs;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path path = Paths.get("pulse_grasp_" + timestamp + ".json");
        lastReportPath = path.toString();

        JsonObject root = new JsonObject();
        root.addProperty("system", "PulseGrasp");
        root.addProperty("durationMs", durationMs);
        root.addProperty("durationSeconds", durationMs / 1000);

        // ====== 记录者信息 ======
        JsonObject recordedBy = new JsonObject();
        JsonObject startBy = new JsonObject();
        startBy.addProperty("name", startName != null ? startName : "unknown");
        startBy.addProperty("uuid", startUuid != null ? startUuid : "none");
        recordedBy.add("start", startBy);
        JsonObject stopBy = new JsonObject();
        stopBy.addProperty("name", stopName != null ? stopName : "unknown");
        stopBy.addProperty("uuid", stopUuid != null ? stopUuid : "none");
        recordedBy.add("stop", stopBy);
        root.add("recordedBy", recordedBy);

        // ====== 第一层：Tick 切脉数据 ======
        JsonObject tickData = tickProfiler.toJson(durationMs);
        // 将 tick 子字段平铺到根
        copyProperty(tickData, root, "graspedTicks");
        copyProperty(tickData, root, "avgTickTimeMs");
        copyProperty(tickData, root, "meridians");
        copyProperty(tickData, root, "entityMeridians");
        copyProperty(tickData, root, "vitalSigns");
        copyProperty(tickData, root, "worldChunks");

        // ====== 第二层：网络数据包脉象（仅调用一次，供融合与 network 段共用） ======
        JsonObject packetData = packetProfiler.toJson(durationMs);

        // ====== 方块实体：融合 tick 耗时 + 网络包数据 ======
        // 从 tick 数据取 blockEntityMeridians（含 tick 耗时和 topConsumers）
        // 从 packet 数据取 blockEntityStats（含网络包字节）
        // 融合：在 blockEntityMeridians 的每个类型条目中注入 packetBytes/packetCount
        JsonObject beMerged = mergeBlockEntityData(
                tickData.getAsJsonArray("blockEntityMeridians"),
                packetData.getAsJsonObject("blockEntityStats")
        );
        root.add("blockEntityMeridians", beMerged);

        // ====== 网络数据包脉象 ======
        JsonObject networkSection = new JsonObject();
        networkSection.addProperty("totalBytes", packetData.get("totalBytes").getAsLong());
        networkSection.addProperty("totalPackets", packetData.get("totalPackets").getAsLong());
        networkSection.addProperty("bytesPerSecond", packetData.get("bytesPerSecond").getAsLong());
        networkSection.addProperty("packetsPerSecond", packetData.get("packetsPerSecond").getAsLong());
        networkSection.add("packetTypes", packetData.get("packetTypes"));
        networkSection.add("playerStats", packetData.get("playerStats"));
        networkSection.add("chunkStats", packetData.get("chunkStats"));
        networkSection.add("flowSeries", packetData.get("flowSeries"));
        // 方块实体网络包数据已融合到 blockEntityMeridians，此处不再重复
        root.add("network", networkSection);

        // ====== 第三层：线程 CPU 脉象（停止时快照） ======
        root.add("threadDump", threadProfiler.capture());

        // ====== 系统资源时间序列（内存 + CPU） ======
        root.add("systemSeries", systemProfiler.toJson());

        // 写入文件
        try (FileWriter writer = new FileWriter(path.toFile())) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 融合方块实体数据：将 tick 耗时（blockEntityMeridians JsonArray）与网络包统计（blockEntityStats JsonObject）合并。
     * 每个类型条目同时包含 tick 性能指标和网络流量指标。
     */
    private JsonObject mergeBlockEntityData(JsonArray tickBeArray, JsonObject packetBeStats) {
        JsonObject merged = new JsonObject();

        // 将 tick 的 JsonArray 转为按 name 查找的 Map
        java.util.Map<String, JsonObject> tickLookup = new java.util.LinkedHashMap<>();
        if (tickBeArray != null) {
            for (int i = 0; i < tickBeArray.size(); i++) {
                JsonObject entry = tickBeArray.get(i).getAsJsonObject();
                tickLookup.put(entry.get("name").getAsString(), entry);
            }
        }

        // 将 packet 的 types 转为按 name 查找的 Map
        java.util.Map<String, JsonObject> packetLookup = new java.util.LinkedHashMap<>();
        if (packetBeStats != null && packetBeStats.has("types")) {
            JsonObject packetTypes = packetBeStats.getAsJsonObject("types");
            for (String key : packetTypes.keySet()) {
                packetLookup.put(key, packetTypes.getAsJsonObject(key));
            }
        }

        // 收集所有类型名（tick 和 packet 的并集）
        java.util.Set<String> allTypes = new java.util.LinkedHashSet<>();
        allTypes.addAll(tickLookup.keySet());
        allTypes.addAll(packetLookup.keySet());

        JsonObject mergedTypes = new JsonObject();

        for (String type : allTypes) {
            JsonObject entry = new JsonObject();

            // tick 数据
            JsonObject tickEntry = tickLookup.get(type);
            if (tickEntry != null) {
                copyProperty(tickEntry, entry, "totalMs");
                copyProperty(tickEntry, entry, "count");
                copyProperty(tickEntry, entry, "avgMs");
                copyProperty(tickEntry, entry, "percentage");
                copyProperty(tickEntry, entry, "topConsumers");
            }

            // 网络包数据
            JsonObject pktEntry = packetLookup.get(type);
            if (pktEntry != null) {
                entry.addProperty("packetBytes", pktEntry.get("bytes").getAsLong());
                entry.addProperty("packetCount", pktEntry.get("packets").getAsLong());
            } else {
                entry.addProperty("packetBytes", 0);
                entry.addProperty("packetCount", 0);
            }

            mergedTypes.add(type, entry);
        }

        // 按 tick 总耗时降序排列（有 tick 数据的排前面），然后输出为 JsonArray
        com.google.gson.JsonArray sortedArray = new com.google.gson.JsonArray();
        allTypes.stream()
                .sorted((a, b) -> {
                    JsonObject aTick = tickLookup.get(a);
                    JsonObject bTick = tickLookup.get(b);
                    boolean aHasTick = aTick != null;
                    boolean bHasTick = bTick != null;
                    if (aHasTick != bHasTick) return aHasTick ? -1 : 1;
                    if (aHasTick) {
                        double aMs = aTick.get("totalMs").getAsDouble();
                        double bMs = bTick.get("totalMs").getAsDouble();
                        return Double.compare(bMs, aMs);
                    }
                    // 都没有 tick 数据，按 packetBytes 降序
                    JsonObject aPkt = packetLookup.get(a);
                    JsonObject bPkt = packetLookup.get(b);
                    long aBytes = aPkt != null ? aPkt.get("bytes").getAsLong() : 0;
                    long bBytes = bPkt != null ? bPkt.get("bytes").getAsLong() : 0;
                    return Long.compare(bBytes, aBytes);
                })
                .forEach(type -> {
                    JsonObject entry = mergedTypes.getAsJsonObject(type);
                    entry.addProperty("name", type);
                    sortedArray.add(entry);
                });
        merged.add("types", sortedArray);

        // 位置级数据（仅保留网络包统计，已有网络包数据）
        if (packetBeStats != null && packetBeStats.has("positions")) {
            merged.add("positions", packetBeStats.get("positions"));
        }

        return merged;
    }

    private void copyProperty(JsonObject src, JsonObject dst, String key) {
        if (src.has(key)) {
            dst.add(key, src.get(key));
        }
    }
}