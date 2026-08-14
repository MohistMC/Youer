package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 网络数据包脉象分析器 — 采集服务端发送的所有数据包统计数据，
 * 按类型、玩家、区块、方块实体等维度聚合，与 TickProfiler 数据融合输出。
 */
public class PacketProfiler {

    // 全局流量统计
    private final AtomicLong totalBytesSent = new AtomicLong(0);
    private final AtomicLong totalPacketsSent = new AtomicLong(0);

    // 每秒流量（滑动窗口）
    private final AtomicLong currentSecondBytes = new AtomicLong(0);
    private final AtomicLong currentSecondPackets = new AtomicLong(0);
    private volatile long bytesPerSecond = 0;
    private volatile long packetsPerSecond = 0;

    // 每秒流量时间序列
    private final java.util.List<FlowSample> flowSamples = new java.util.ArrayList<>();

    // 按数据包类型统计
    private final Map<String, AtomicLong> bytesByPacketType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> packetsByPacketType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> currentSecondBytesByPacketType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> currentSecondPacketsByPacketType = new ConcurrentHashMap<>();
    private final Map<String, Long> bytesPerSecondByPacketType = new ConcurrentHashMap<>();
    private final Map<String, Long> packetsPerSecondByPacketType = new ConcurrentHashMap<>();

    // 按区块统计（ClientboundLevelChunkWithLightPacket）
    private final Map<String, AtomicLong> chunkBytes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> chunkPackets = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> chunkPlayers = new ConcurrentHashMap<>();

    // 按玩家统计
    private final Map<String, AtomicLong> playerBytes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> playerPackets = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> packetTypePlayers = new ConcurrentHashMap<>();

    // 按方块实体统计（ClientboundBlockEntityDataPacket）
    // 按坐标
    private final Map<String, AtomicLong> bePosBytes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> bePosPackets = new ConcurrentHashMap<>();
    private final Map<String, String> bePosTypes = new ConcurrentHashMap<>();
    // 按类型
    private final Map<String, AtomicLong> beTypeBytes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> beTypePackets = new ConcurrentHashMap<>();

    private volatile long lastSecond = System.currentTimeMillis() / 1000;
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
                    updateSecondlyStats();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "PulseGrasp-Packet-Updater");
        updaterThread.setDaemon(true);
        updaterThread.start();
    }

    void stop() {
        running = false;
        if (updaterThread != null) {
            updaterThread.interrupt();
            updaterThread = null;
        }
        flushRemainingFlow();
    }

    public void reset() {
        totalBytesSent.set(0);
        totalPacketsSent.set(0);
        currentSecondBytes.set(0);
        currentSecondPackets.set(0);
        bytesPerSecond = 0;
        packetsPerSecond = 0;
        flowSamples.clear();
        bytesByPacketType.clear();
        packetsByPacketType.clear();
        currentSecondBytesByPacketType.clear();
        currentSecondPacketsByPacketType.clear();
        bytesPerSecondByPacketType.clear();
        packetsPerSecondByPacketType.clear();
        chunkBytes.clear();
        chunkPackets.clear();
        chunkPlayers.clear();
        playerBytes.clear();
        playerPackets.clear();
        packetTypePlayers.clear();
        bePosBytes.clear();
        bePosPackets.clear();
        bePosTypes.clear();
        beTypeBytes.clear();
        beTypePackets.clear();
    }

    // ---- 更新钩子（由 PacketEncoder 补丁调用） ----

    /** 更新数据包统计 */
    public void updatePacketStats(String packetClassName, int bytes, String playerName) {
        totalBytesSent.addAndGet(bytes);
        totalPacketsSent.incrementAndGet();
        currentSecondBytes.addAndGet(bytes);
        currentSecondPackets.incrementAndGet();

        bytesByPacketType.computeIfAbsent(packetClassName, k -> new AtomicLong(0)).addAndGet(bytes);
        packetsByPacketType.computeIfAbsent(packetClassName, k -> new AtomicLong(0)).incrementAndGet();
        currentSecondBytesByPacketType.computeIfAbsent(packetClassName, k -> new AtomicLong(0)).addAndGet(bytes);
        currentSecondPacketsByPacketType.computeIfAbsent(packetClassName, k -> new AtomicLong(0)).incrementAndGet();

        if (playerName != null) {
            playerBytes.computeIfAbsent(playerName, k -> new AtomicLong(0)).addAndGet(bytes);
            playerPackets.computeIfAbsent(playerName, k -> new AtomicLong(0)).incrementAndGet();
            packetTypePlayers.computeIfAbsent(packetClassName, k -> ConcurrentHashMap.newKeySet()).add(playerName);
        }
    }

    /** 更新区块数据包统计 */
    public void updateChunkPacketStats(String world, int x, int z, int bytes, String playerName) {
        String key = world + ":" + x + "," + z;
        chunkBytes.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(bytes);
        chunkPackets.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        if (playerName != null) {
            chunkPlayers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(playerName);
        }
    }

    /** 更新方块实体数据包统计 */
    public void updateBlockEntityStats(String world, int x, int y, int z, String type, int bytes) {
        String posKey = world + ":" + x + "," + y + "," + z;
        bePosBytes.computeIfAbsent(posKey, k -> new AtomicLong(0)).addAndGet(bytes);
        bePosPackets.computeIfAbsent(posKey, k -> new AtomicLong(0)).incrementAndGet();
        bePosTypes.putIfAbsent(posKey, type);
        beTypeBytes.computeIfAbsent(type, k -> new AtomicLong(0)).addAndGet(bytes);
        beTypePackets.computeIfAbsent(type, k -> new AtomicLong(0)).incrementAndGet();
    }

    // ---- 内部 ----

    private void updateSecondlyStats() {
        long currentSecond = System.currentTimeMillis() / 1000;
        if (currentSecond > lastSecond) {
            long bytes = currentSecondBytes.getAndSet(0);
            long packets = currentSecondPackets.getAndSet(0);
            bytesPerSecond = bytes;
            packetsPerSecond = packets;
            lastSecond = currentSecond;
            flowSamples.add(new FlowSample(currentSecond, bytes, packets));

            bytesPerSecondByPacketType.clear();
            packetsPerSecondByPacketType.clear();
            for (Map.Entry<String, AtomicLong> entry : currentSecondBytesByPacketType.entrySet()) {
                bytesPerSecondByPacketType.put(entry.getKey(), entry.getValue().getAndSet(0));
            }
            for (Map.Entry<String, AtomicLong> entry : currentSecondPacketsByPacketType.entrySet()) {
                packetsPerSecondByPacketType.put(entry.getKey(), entry.getValue().getAndSet(0));
            }
        }
    }

    /** 停止时补录最后一秒尚未滚动的流量 */
    private void flushRemainingFlow() {
        long bytes = currentSecondBytes.getAndSet(0);
        long packets = currentSecondPackets.getAndSet(0);
        if (bytes > 0 || packets > 0) {
            flowSamples.add(new FlowSample(System.currentTimeMillis() / 1000, bytes, packets));
        }
    }

    /** 一次每秒流量采样 */
    private static class FlowSample {
        final long timestamp;
        final long bytes;
        final long packets;

        FlowSample(long timestamp, long bytes, long packets) {
            this.timestamp = timestamp;
            this.bytes = bytes;
            this.packets = packets;
        }
    }

    // ---- 查询接口 ----

    public long getTotalBytes() { return totalBytesSent.get(); }
    public long getTotalPackets() { return totalPacketsSent.get(); }
    public long getBytesPerSecond() { return bytesPerSecond; }
    public long getPacketsPerSecond() { return packetsPerSecond; }

    public Map<String, Long> getBytesByPacketType() {
        return bytesByPacketType.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    public Map<String, Long> getPacketsByPacketType() {
        return packetsByPacketType.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    public long getBytesPerSecondByPacketType(String packetType) {
        return bytesPerSecondByPacketType.getOrDefault(packetType, 0L);
    }

    public long getPacketsPerSecondByPacketType(String packetType) {
        return packetsPerSecondByPacketType.getOrDefault(packetType, 0L);
    }

    // ---- JSON 输出 ----

    JsonObject toJson(long totalDurationMs) {
        JsonObject root = new JsonObject();
        root.addProperty("totalBytes", totalBytesSent.get());
        root.addProperty("totalPackets", totalPacketsSent.get());
        root.addProperty("bytesPerSecond", bytesPerSecond);
        root.addProperty("packetsPerSecond", packetsPerSecond);

        // 每秒流量时间序列
        JsonArray flowSeries = new JsonArray();
        for (FlowSample sample : flowSamples) {
            JsonObject obj = new JsonObject();
            obj.addProperty("timestamp", sample.timestamp);
            obj.addProperty("bytes", sample.bytes);
            obj.addProperty("packets", sample.packets);
            flowSeries.add(obj);
        }
        root.add("flowSeries", flowSeries);

        // 按数据包类型
        JsonObject packetTypes = new JsonObject();
        getBytesByPacketType().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String packetType = entry.getKey();
                    long bytes = entry.getValue();
                    long packets = getPacketsByPacketType().getOrDefault(packetType, 0L);
                    long bps = getBytesPerSecondByPacketType(packetType);
                    long pps = getPacketsPerSecondByPacketType(packetType);

                    JsonObject stats = new JsonObject();
                    stats.addProperty("bytes", bytes);
                    stats.addProperty("packets", packets);
                    stats.addProperty("avgBytesPerPacket", packets > 0 ? bytes / packets : 0);
                    stats.addProperty("bandwidthPercentage", totalBytesSent.get() > 0 ? (double) bytes / totalBytesSent.get() * 100 : 0);
                    stats.addProperty("bytesPerSecond", bps);
                    stats.addProperty("packetsPerSecond", pps);

                    JsonArray players = new JsonArray();
                    Set<String> playerSet = packetTypePlayers.get(packetType);
                    if (playerSet != null) playerSet.forEach(players::add);
                    stats.add("players", players);

                    packetTypes.add(packetType, stats);
                });
        root.add("packetTypes", packetTypes);

        // 按玩家
        JsonObject playerStats = new JsonObject();
        playerBytes.forEach((name, bytes) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("bytes", bytes.get());
            entry.addProperty("packets", playerPackets.getOrDefault(name, new AtomicLong(0)).get());
            playerStats.add(name, entry);
        });
        root.add("playerStats", playerStats);

        // 按区块
        JsonObject chunkStats = new JsonObject();
        chunkBytes.forEach((pos, bytes) -> {
            JsonObject chunkEntry = new JsonObject();
            chunkEntry.addProperty("bytes", bytes.get());
            chunkEntry.addProperty("packets", chunkPackets.getOrDefault(pos, new AtomicLong(0)).get());
            JsonArray players = new JsonArray();
            Set<String> playerSet = chunkPlayers.get(pos);
            if (playerSet != null) playerSet.forEach(players::add);
            chunkEntry.add("players", players);
            chunkStats.add(pos, chunkEntry);
        });
        root.add("chunkStats", chunkStats);

        // 按方块实体（位置 + 类型）
        JsonObject blockEntityStats = new JsonObject();
        JsonObject bePositions = new JsonObject();
        bePosBytes.forEach((pos, bytes) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("bytes", bytes.get());
            entry.addProperty("packets", bePosPackets.getOrDefault(pos, new AtomicLong(0)).get());
            entry.addProperty("type", bePosTypes.getOrDefault(pos, "unknown"));
            bePositions.add(pos, entry);
        });
        blockEntityStats.add("positions", bePositions);

        JsonObject beTypes = new JsonObject();
        beTypeBytes.forEach((type, bytes) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("bytes", bytes.get());
            entry.addProperty("packets", beTypePackets.getOrDefault(type, new AtomicLong(0)).get());
            beTypes.add(type, entry);
        });
        blockEntityStats.add("types", beTypes);
        root.add("blockEntityStats", blockEntityStats);

        return root;
    }
}