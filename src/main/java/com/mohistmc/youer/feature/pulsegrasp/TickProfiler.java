package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;

/**
 * Tick 切脉分析器 — 采集服务器每 tick 各环节耗时、实体/方块实体类型耗时及生命体征。
 */
public class TickProfiler {

    private static final int TOP_CONSUMER_LIMIT = 10;

    // 环节耗时
    private final Map<String, Long> meridianTimes = new LinkedHashMap<>();
    private final Map<String, Integer> meridianCounts = new LinkedHashMap<>();

    // 实体耗时
    private final Map<String, Long> entityMeridianTimes = new LinkedHashMap<>();
    private final Map<String, Integer> entityMeridianCounts = new LinkedHashMap<>();
    private final Map<String, Map<UUID, EntityTrace>> entityInstanceTraces = new HashMap<>();

    // 方块实体耗时
    private final Map<String, Long> blockEntityMeridianTimes = new LinkedHashMap<>();
    private final Map<String, Integer> blockEntityMeridianCounts = new LinkedHashMap<>();
    private final Map<String, Map<String, BlockEntityTrace>> blockEntityInstanceTraces = new HashMap<>();

    // 生命体征
    private final java.util.List<VitalSign> vitalSigns = new java.util.ArrayList<>();

    // 区块统计（按维度）
    private final Map<String, ChunkStat> chunkStats = new LinkedHashMap<>();

    // 当前切脉状态
    private String currentMeridian;
    private long meridianStartNs;
    private String currentDimension; // 当前维度上下文，用于 meridian 名前缀

    private int tickCount;

    // ---- 生命周期 ----

    void reset() {
        tickCount = 0;
        meridianTimes.clear();
        meridianCounts.clear();
        entityMeridianTimes.clear();
        entityMeridianCounts.clear();
        blockEntityMeridianTimes.clear();
        blockEntityMeridianCounts.clear();
        entityInstanceTraces.clear();
        blockEntityInstanceTraces.clear();
        vitalSigns.clear();
        chunkStats.clear();
        currentMeridian = null;
    }

    // ---- 对外接口 ----

    public int getTickCount() {
        return tickCount;
    }

    /** 记录一次 tick 完成 — 同时采集生命体征 */
    public void markTick() {
        tickCount++;
        vitalSigns.add(new VitalSign(
                System.currentTimeMillis(),
                Math.max(Math.min(Bukkit.getTPS()[0], 20.0D), 0.0D),
                Bukkit.getAverageTickTime(),
                calcAveragePing()
        ));
    }

    private int calcAveragePing() {
        int total = 0;
        int count = 0;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            total += p.getPing();
            count++;
        }
        return count > 0 ? total / count : 0;
    }

    /** 设置当前维度上下文 — 随后的 feelPulse 会自动添加维度前缀 */
    public void setLevel(String dimension) {
        currentDimension = dimension;
    }

    /** 开始切脉 — 进入某个环节（自动添加维度前缀） */
    public void feelPulse(String meridian) {
        if (currentMeridian != null) {
            pulseComplete();
        }
        currentMeridian = currentDimension != null ? currentDimension + ":" + meridian : meridian;
        meridianStartNs = System.nanoTime();
    }

    /** 切脉结束 */
    public void pulseComplete() {
        if (currentMeridian == null) return;
        long elapsed = System.nanoTime() - meridianStartNs;
        meridianTimes.merge(currentMeridian, elapsed, Long::sum);
        meridianCounts.merge(currentMeridian, 1, Integer::sum);
        currentMeridian = null;
    }

    /** 记录实体 tick 耗时（自动添加维度前缀） */
    public void recordEntityPulse(String entityType, long nanos, UUID uuid, String world, int x, int y, int z) {
        String key = currentDimension != null ? currentDimension + "@@" + entityType : entityType;
        entityMeridianTimes.merge(key, nanos, Long::sum);
        entityMeridianCounts.merge(key, 1, Integer::sum);
        entityInstanceTraces
                .computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(uuid, k -> new EntityTrace(uuid, world, x, y, z))
                .accumulate(nanos);
    }

    /** 记录方块实体 tick 耗时（自动添加维度前缀） */
    public void recordBlockEntityPulse(String blockEntityType, long nanos, String world, int x, int y, int z) {
        String key = currentDimension != null ? currentDimension + ":" + blockEntityType : blockEntityType;
        blockEntityMeridianTimes.merge(key, nanos, Long::sum);
        blockEntityMeridianCounts.merge(key, 1, Integer::sum);
        String posKey = world + ":" + x + "," + y + "," + z;
        blockEntityInstanceTraces
                .computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(posKey, k -> new BlockEntityTrace(blockEntityType, world, x, y, z))
                .accumulate(nanos);
    }

    /** 记录当前维度的区块统计（总区块数与活跃区块数） */
    public void recordChunkStat(int totalChunks, int activeChunks) {
        if (currentDimension == null) return;
        chunkStats.computeIfAbsent(currentDimension, k -> new ChunkStat()).accumulate(totalChunks, activeChunks);
    }

    // ---- JSON 输出 ----

    JsonObject toJson(long totalDurationMs) {
        JsonObject root = new JsonObject();
        root.addProperty("graspedTicks", tickCount);
        root.addProperty("avgTickTimeMs", tickCount > 0 ? String.format("%.2f", (double) totalDurationMs / tickCount) : "0");

        // 环节脉象
        root.add("meridians", buildSortedJsonArray(meridianTimes, meridianCounts, totalDurationMs));

        // 实体脉象（含实例下钻）
        @SuppressWarnings("unchecked")
        Map<String, Map<Object, ? extends TraceData>> entityTraces = (Map<String, Map<Object, ? extends TraceData>>) (Map<?, ?>) entityInstanceTraces;
        root.add("entityMeridians", buildEntityJsonArray(entityMeridianTimes, entityMeridianCounts, entityTraces, totalDurationMs,
                (obj, trace) -> obj.addProperty("uuid", ((EntityTrace) trace).uuid.toString())));

        // 方块实体脉象（含实例下钻）
        @SuppressWarnings("unchecked")
        Map<String, Map<Object, ? extends TraceData>> blockEntityTraces = (Map<String, Map<Object, ? extends TraceData>>) (Map<?, ?>) blockEntityInstanceTraces;
        root.add("blockEntityMeridians", buildEntityJsonArray(blockEntityMeridianTimes, blockEntityMeridianCounts, blockEntityTraces, totalDurationMs,
                (obj, trace) -> obj.addProperty("type", ((BlockEntityTrace) trace).type)));

        // 生命体征
        JsonArray vitalArray = new JsonArray();
        for (VitalSign vs : vitalSigns) {
            JsonObject obj = new JsonObject();
            obj.addProperty("timestamp", vs.timestamp);
            obj.addProperty("tps", vs.tps);
            obj.addProperty("mspt", vs.mspt);
            obj.addProperty("ping", vs.ping);
            vitalArray.add(obj);
        }
        root.add("vitalSigns", vitalArray);

        // 世界区块统计
        root.add("worldChunks", buildChunkJsonArray());

        return root;
    }

    private JsonArray buildChunkJsonArray() {
        JsonArray array = new JsonArray();
        for (Map.Entry<String, ChunkStat> entry : chunkStats.entrySet()) {
            ChunkStat stat = entry.getValue();
            JsonObject obj = new JsonObject();
            obj.addProperty("dimension", entry.getKey());
            obj.addProperty("totalChunks", stat.latestTotal);
            obj.addProperty("activeChunks", stat.latestActive);
            obj.addProperty("avgTotalChunks", String.format("%.1f", (double) stat.totalSum / stat.samples));
            obj.addProperty("avgActiveChunks", String.format("%.1f", (double) stat.activeSum / stat.samples));
            obj.addProperty("maxTotalChunks", stat.maxTotal);
            obj.addProperty("maxActiveChunks", stat.maxActive);
            obj.addProperty("samples", stat.samples);
            array.add(obj);
        }
        return array;
    }

    private JsonArray buildSortedJsonArray(Map<String, Long> times, Map<String, Integer> counts, long totalDurationMs) {
        List<Map.Entry<String, Long>> sorted = times.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .toList();
        JsonArray array = new JsonArray();
        for (Map.Entry<String, Long> entry : sorted) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", entry.getKey());
            obj.addProperty("totalMs", String.format("%.2f", entry.getValue() / 1_000_000.0));
            obj.addProperty("count", counts.getOrDefault(entry.getKey(), 0));
            obj.addProperty("avgMs", String.format("%.4f", entry.getValue() / 1_000_000.0 / Math.max(1, counts.getOrDefault(entry.getKey(), 0))));
            obj.addProperty("percentage", totalDurationMs > 0 ? String.format("%.2f", entry.getValue() / 1_000_000.0 / totalDurationMs * 100) : "0.00");
            array.add(obj);
        }
        return array;
    }

    private JsonArray buildEntityJsonArray(
            Map<String, Long> times,
            Map<String, Integer> counts,
            Map<String, Map<Object, ? extends TraceData>> instanceTraces,
            long totalDurationMs,
            BiConsumer<JsonObject, TraceData> identityWriter) {
        List<Map.Entry<String, Long>> sorted = times.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
        JsonArray array = new JsonArray();
        for (Map.Entry<String, Long> entry : sorted) {
            JsonObject typeObj = new JsonObject();
            String key = entry.getKey();
            int dimIdx = key.indexOf("@@");
            if (dimIdx >= 0) {
                typeObj.addProperty("name", key.substring(dimIdx + 2));
                typeObj.addProperty("dimension", key.substring(0, dimIdx));
            } else {
                typeObj.addProperty("name", key);
                typeObj.addProperty("dimension", "");
            }
            typeObj.addProperty("totalMs", String.format("%.2f", entry.getValue() / 1_000_000.0));
            typeObj.addProperty("count", counts.getOrDefault(entry.getKey(), 0));
            typeObj.addProperty("avgMs", String.format("%.4f", entry.getValue() / 1_000_000.0 / Math.max(1, counts.getOrDefault(entry.getKey(), 0))));
            typeObj.addProperty("percentage", totalDurationMs > 0 ? String.format("%.2f", entry.getValue() / 1_000_000.0 / totalDurationMs * 100) : "0.00");

            // 实例下钻（无数据时输出空数组，保证前端 schema 稳定）
            JsonArray consumers = new JsonArray();
            Map<Object, ? extends TraceData> instances = instanceTraces.get(entry.getKey());
            if (instances != null && !instances.isEmpty()) {
                instances.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue().totalNanos(), a.getValue().totalNanos()))
                        .limit(TOP_CONSUMER_LIMIT)
                        .forEach(e -> {
                            TraceData trace = e.getValue();
                            JsonObject obj = new JsonObject();
                            identityWriter.accept(obj, trace);
                            obj.addProperty("world", trace.world());
                            obj.addProperty("pos", trace.x() + "," + trace.y() + "," + trace.z());
                            obj.addProperty("totalMs", String.format("%.2f", trace.totalNanos() / 1_000_000.0));
                            obj.addProperty("count", trace.count());
                            obj.addProperty("avgMs", String.format("%.4f", trace.totalNanos() / 1_000_000.0 / Math.max(1, trace.count())));
                            consumers.add(obj);
                        });
            }
            typeObj.add("topConsumers", consumers);
            array.add(typeObj);
        }
        return array;
    }
}