package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;

/**
 * Tick profiler — collects per-tick timing, entity/block-entity type timing, and vital signs.
 */
public class TickProfiler {

    private static final int TOP_CONSUMER_LIMIT = 10;

    /** Identity writer for drill-down records — attaches the type name for self-identification */
    @FunctionalInterface
    private interface TypeIdentityWriter {
        void write(JsonObject obj, TraceData trace, String typeName);
    }

    // meridian timing
    private final Map<String, Long> meridianTimes = new LinkedHashMap<>();
    private final Map<String, Integer> meridianCounts = new LinkedHashMap<>();

    // entity timing
    private final Map<String, Long> entityMeridianTimes = new LinkedHashMap<>();
    private final Map<String, Integer> entityMeridianCounts = new LinkedHashMap<>();
    private final Map<String, Map<UUID, EntityTrace>> entityInstanceTraces = new HashMap<>();

    // block entity timing
    private final Map<String, Long> blockEntityMeridianTimes = new LinkedHashMap<>();
    private final Map<String, Integer> blockEntityMeridianCounts = new LinkedHashMap<>();
    private final Map<String, Map<String, BlockEntityTrace>> blockEntityInstanceTraces = new HashMap<>();

    // vital signs
    private final java.util.List<VitalSign> vitalSigns = new java.util.ArrayList<>();

    // chunk statistics (per dimension)
    private final Map<String, ChunkStat> chunkStats = new LinkedHashMap<>();

    // current profiling state
    private String currentMeridian;
    private long meridianStartNs;
    private String currentDimension; // current dimension context for meridian name prefix

    private int tickCount;

    // ---- Lifecycle ----

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

    // ---- Public API ----

    public int getTickCount() {
        return tickCount;
    }

    /** Record a tick completion — also samples vital signs */
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

    /** Set current dimension context — subsequent feelPulse calls add a dimension prefix */
    public void setLevel(String dimension) {
        currentDimension = dimension;
    }

    /** Start timing a meridian (auto-adds dimension prefix) */
    public void feelPulse(String meridian) {
        if (currentMeridian != null) {
            pulseComplete();
        }
        currentMeridian = currentDimension != null ? currentDimension + ":" + meridian : meridian;
        meridianStartNs = System.nanoTime();
    }

    /** End meridian timing */
    public void pulseComplete() {
        if (currentMeridian == null) return;
        long elapsed = System.nanoTime() - meridianStartNs;
        meridianTimes.merge(currentMeridian, elapsed, Long::sum);
        meridianCounts.merge(currentMeridian, 1, Integer::sum);
        currentMeridian = null;
    }

    /** Record entity tick time (auto-adds dimension prefix) */
    public void recordEntityPulse(String entityType, long nanos, UUID uuid, String world, int x, int y, int z) {
        String key = currentDimension != null ? currentDimension + "@@" + entityType : entityType;
        entityMeridianTimes.merge(key, nanos, Long::sum);
        entityMeridianCounts.merge(key, 1, Integer::sum);
        EntityTrace entityTrace = entityInstanceTraces
                .computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(uuid, k -> new EntityTrace(uuid, world, x, y, z));
        // refresh position on move to keep records accurate
        entityTrace.updatePosition(world, x, y, z);
        entityTrace.accumulate(nanos);
    }

    /** Record block entity tick time (auto-adds dimension prefix) */
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

    /** Record chunk statistics for the current dimension (total and active chunks) */
    public void recordChunkStat(int totalChunks, int activeChunks) {
        if (currentDimension == null) return;
        chunkStats.computeIfAbsent(currentDimension, k -> new ChunkStat()).accumulate(totalChunks, activeChunks);
    }

    // ---- JSON output ----

    JsonObject toJson(long totalDurationMs) {
        JsonObject root = new JsonObject();
        root.addProperty("graspedTicks", tickCount);
        root.addProperty("avgTickTimeMs", tickCount > 0 ? String.format("%.2f", (double) totalDurationMs / tickCount) : "0");

        // meridian data
        root.add("meridians", buildSortedJsonArray(meridianTimes, meridianCounts, totalDurationMs));

        // entity data (with instance drill-down)
        @SuppressWarnings("unchecked")
        Map<String, Map<Object, ? extends TraceData>> entityTraces = (Map<String, Map<Object, ? extends TraceData>>) (Map<?, ?>) entityInstanceTraces;
        root.add("entityMeridians", buildEntityJsonArray(entityMeridianTimes, entityMeridianCounts, entityTraces, totalDurationMs,
                (obj, trace, typeName) -> {
                    obj.addProperty("uuid", ((EntityTrace) trace).uuid.toString());
                    obj.addProperty("type", typeName);
                }));

        // block entity data (with instance drill-down)
        @SuppressWarnings("unchecked")
        Map<String, Map<Object, ? extends TraceData>> blockEntityTraces = (Map<String, Map<Object, ? extends TraceData>>) (Map<?, ?>) blockEntityInstanceTraces;
        root.add("blockEntityMeridians", buildEntityJsonArray(blockEntityMeridianTimes, blockEntityMeridianCounts, blockEntityTraces, totalDurationMs,
                (obj, trace, typeName) -> {
                    obj.addProperty("type", ((BlockEntityTrace) trace).type);
                    obj.addProperty("world", trace.world());
                }));

        // vital signs
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

        // world chunk statistics
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
            TypeIdentityWriter identityWriter) {
        List<Map.Entry<String, Long>> sorted = times.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
        JsonArray array = new JsonArray();
        for (Map.Entry<String, Long> entry : sorted) {
            JsonObject typeObj = new JsonObject();
            String key = entry.getKey();
            String name;
            int dimIdx = key.indexOf("@@");
            if (dimIdx >= 0) {
                name = key.substring(dimIdx + 2);
                typeObj.addProperty("name", name);
                typeObj.addProperty("dimension", key.substring(0, dimIdx));
            } else {
                name = key;
                typeObj.addProperty("name", name);
                typeObj.addProperty("dimension", "");
            }
            typeObj.addProperty("totalMs", String.format("%.2f", entry.getValue() / 1_000_000.0));
            typeObj.addProperty("count", counts.getOrDefault(entry.getKey(), 0));
            typeObj.addProperty("avgMs", String.format("%.4f", entry.getValue() / 1_000_000.0 / Math.max(1, counts.getOrDefault(entry.getKey(), 0))));
            typeObj.addProperty("percentage", totalDurationMs > 0 ? String.format("%.2f", entry.getValue() / 1_000_000.0 / totalDurationMs * 100) : "0.00");

            // instance drill-down (empty array when no data, keeps front-end schema stable)
            JsonArray consumers = new JsonArray();
            Map<Object, ? extends TraceData> instances = instanceTraces.get(entry.getKey());
            if (instances != null && !instances.isEmpty()) {
                instances.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue().totalNanos(), a.getValue().totalNanos()))
                        .limit(TOP_CONSUMER_LIMIT)
                        .forEach(e -> {
                            TraceData trace = e.getValue();
                            JsonObject obj = new JsonObject();
                            identityWriter.write(obj, trace, name);
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