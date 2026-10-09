package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.Bukkit;

/**
 * Per-tick timing, entity/block entity timing and vital signs.
 * Keeps both session totals and a snapshot of the slowest ticks, so a spike can be
 * traced back to what was actually running during it.
 */
public class TickProfiler {

    private static final int TOP_CONSUMER_LIMIT = 10;

    /** Slowest ticks kept with a full per-tick breakdown. */
    private static final int WORST_TICK_LIMIT = 20;
    /** 50ms is the whole 20 TPS budget, so anything above it is a slow tick. */
    private static final long SLOW_TICK_THRESHOLD_NANOS = 50_000_000L;
    /** Ring buffer for percentiles: 12000 ticks is 10 minutes at 20 TPS. */
    private static final int TICK_RING_SIZE = 12000;
    /** Ping is a slow-moving value; sampling it every tick would walk every player for nothing. */
    private static final int PING_SAMPLE_INTERVAL_TICKS = 20;
    /** Bounds the vital-sign series so long sessions cannot grow forever. */
    private static final int VITALS_LIMIT = 72000;
    /** Upper bound of each histogram bucket, in ms. */
    private static final long[] HIST_EDGES_MS = {10, 20, 30, 40, 50, 75, 100, 150, 200, 300, 500, 1000, Long.MAX_VALUE};

    /** Attaches the type name to a drill-down record. */
    @FunctionalInterface
    private interface TypeIdentityWriter {
        void write(JsonObject obj, TraceData trace, String typeName);
    }

    // session totals
    private final Map<String, Long> meridianTimes = new LinkedHashMap<>();
    private final Map<String, Integer> meridianCounts = new LinkedHashMap<>();
    private final Map<String, Long> entityMeridianTimes = new LinkedHashMap<>();
    private final Map<String, Integer> entityMeridianCounts = new LinkedHashMap<>();
    private final Map<String, Map<UUID, EntityTrace>> entityInstanceTraces = new HashMap<>();
    private final Map<String, Long> blockEntityMeridianTimes = new LinkedHashMap<>();
    private final Map<String, Integer> blockEntityMeridianCounts = new LinkedHashMap<>();
    private final Map<String, Map<String, BlockEntityTrace>> blockEntityInstanceTraces = new HashMap<>();
    private final Map<String, Long> chunkSourceTimes = new LinkedHashMap<>();
    private final Map<String, Integer> chunkSourceCounts = new LinkedHashMap<>();
    private final Map<String, Long> blockEventTimes = new LinkedHashMap<>();
    private final Map<String, Integer> blockEventCounts = new LinkedHashMap<>();

    // player movement and the chunk loads it triggers
    private final Map<UUID, PlayerTrail> playerTrails = new LinkedHashMap<>();
    /**
     * Chunks that gained a PLAYER ticket, grouped by dimension. Chunk coordinates are only
     * meaningful inside their own dimension — the nether and the overworld both have a (0,0), so
     * mixing them would draw heat in the wrong place and stretch the map bounds.
     */
    private final Map<String, LongSet> ticketedChunks = new LinkedHashMap<>();

    // current tick only, cleared in markTick
    private final Map<String, Long> tickMeridianTimes = new HashMap<>();
    private final Map<String, Long> tickEntityTimes = new HashMap<>();
    private final Map<String, Integer> tickEntityCounts = new HashMap<>();
    private final Map<String, Long> tickBlockEntityTimes = new HashMap<>();
    private final Map<String, Integer> tickBlockEntityCounts = new HashMap<>();
    private final Map<String, Long> tickChunkSourceTimes = new HashMap<>();
    private final Map<String, Integer> tickChunkSourceCounts = new HashMap<>();
    private final Map<String, Long> tickBlockEventTimes = new HashMap<>();
    private final Map<String, Integer> tickBlockEventCounts = new HashMap<>();

    private final java.util.ArrayDeque<VitalSign> vitalSigns = new java.util.ArrayDeque<>();
    private final Map<String, ChunkStat> chunkStats = new LinkedHashMap<>();

    private long tickNanosSum;
    private long tickNanosMin = Long.MAX_VALUE;
    private long tickNanosMax;
    private final long[] tickRing = new long[TICK_RING_SIZE];
    private int tickRingIndex;
    private int tickRingCount;
    private final long[] histogram = new long[HIST_EDGES_MS.length];
    private int slowTickCount;
    /** Ticks we counted in each complete wall-clock second — the real TPS, measured by us. */
    private final List<Integer> ticksPerSecond = new ArrayList<>();
    private long secondStartMs;
    private int ticksThisSecond;
    /** Ping is a slow-moving value, so it is sampled every {@link #PING_SAMPLE_INTERVAL_TICKS}. */
    private int cachedPing;
    private int pingCountdown;
    /** The configured tick rate, so a low TPS caused by {@code /tick rate} is not mistaken for a fault. */
    private float tickRate = 20.0F;
    private boolean frozen;
    private boolean sprinting;
    /** Main-thread wait for async work, measured outside the tick — see PulseGrasp.recordAsyncWait. */
    private long asyncWaitNanosSum;
    private long asyncWaitNanosMax;
    private int asyncWaitCount;
    /** Min-heap by duration, so the cheapest entry is always the one to evict. */
    private final PriorityQueue<SlowTick> worstTicks =
            new PriorityQueue<>((SlowTick a, SlowTick b) -> Long.compare(a.durationNanos, b.durationNanos));

    private String currentMeridian;
    private long meridianStartNs;
    private String currentDimension;
    private int tickCount;

    void reset() {
        tickCount = 0;
        meridianTimes.clear();
        meridianCounts.clear();
        tickMeridianTimes.clear();
        entityMeridianTimes.clear();
        entityMeridianCounts.clear();
        tickEntityTimes.clear();
        tickEntityCounts.clear();
        blockEntityMeridianTimes.clear();
        blockEntityMeridianCounts.clear();
        tickBlockEntityTimes.clear();
        tickBlockEntityCounts.clear();
        chunkSourceTimes.clear();
        chunkSourceCounts.clear();
        blockEventTimes.clear();
        blockEventCounts.clear();
        tickChunkSourceTimes.clear();
        tickChunkSourceCounts.clear();
        tickBlockEventTimes.clear();
        tickBlockEventCounts.clear();
        entityInstanceTraces.clear();
        blockEntityInstanceTraces.clear();
        vitalSigns.clear();
        chunkStats.clear();
        playerTrails.clear();
        ticketedChunks.clear();
        currentMeridian = null;
        currentDimension = null;
        tickNanosSum = 0;
        tickNanosMin = Long.MAX_VALUE;
        tickNanosMax = 0;
        tickRingIndex = 0;
        tickRingCount = 0;
        Arrays.fill(tickRing, 0L);
        Arrays.fill(histogram, 0L);
        slowTickCount = 0;
        worstTicks.clear();
        ticksPerSecond.clear();
        secondStartMs = 0L;
        ticksThisSecond = 0;
        cachedPing = 0;
        pingCountdown = 0;
        tickRate = 20.0F;
        frozen = false;
        sprinting = false;
        asyncWaitNanosSum = 0;
        asyncWaitNanosMax = 0;
        asyncWaitCount = 0;
    }

    public int getTickCount() {
        return tickCount;
    }

    /** Record a tick with no externally measured duration. */
    public void markTick() {
        markTick(-1L);
    }

    /**
     * Record a tick completion.
     *
     * @param tickNanos the tick's real execution time in nanos; {@code <= 0} falls back to the
     *                  instrumented busy time
     */
    public void markTick(long tickNanos) {
        markTick(tickNanos, 20.0F, false, false);
    }

    /**
     * Record a tick completion.
     *
     * @param tickNanos the tick's real execution time in nanos; {@code <= 0} falls back to the
     *                  instrumented busy time
     * @param tickRate  the configured tick rate — {@code /tick rate} can lower it, and then a low
     *                  TPS is by design rather than a symptom
     */
    public void markTick(long tickNanos, float tickRate, boolean frozen, boolean sprinting) {
        long nanos = tickNanos > 0 ? tickNanos : sumOf(tickMeridianTimes);
        long now = System.currentTimeMillis();

        this.tickRate = tickRate;
        this.frozen = frozen;
        this.sprinting = sprinting;

        // On this server getTPS()[0] is the 5-second time-weighted average (Purpur moved it there),
        // not the 1-minute average the Bukkit API documents. It is the most responsive value
        // available, but it still dilutes a short stall — which is why the exact per-second count
        // below is recorded alongside it.
        double serverTps = Math.max(Math.min(Bukkit.getTPS()[0], 20.0D), 0.0D);
        double mspt = Bukkit.getAverageTickTime();
        int ping = calcAveragePing();

        if (vitalSigns.size() >= VITALS_LIMIT) {
            vitalSigns.removeFirst();
        }
        vitalSigns.addLast(new VitalSign(now, serverTps, mspt, ping));

        countTickIntoSecond(now);

        if (nanos > 0) {
            tickNanosSum += nanos;
            if (nanos < tickNanosMin) tickNanosMin = nanos;
            if (nanos > tickNanosMax) tickNanosMax = nanos;
            tickRing[tickRingIndex] = nanos;
            tickRingIndex = (tickRingIndex + 1) % TICK_RING_SIZE;
            tickRingCount++;
            recordHistogram(nanos);
            if (nanos >= SLOW_TICK_THRESHOLD_NANOS) slowTickCount++;

            if (worstTicks.size() < WORST_TICK_LIMIT
                    || nanos > worstTicks.peek().durationNanos) {
                SlowTick snapshot = new SlowTick(
                        tickCount + 1,
                        now,
                        nanos,
                        serverTps,
                        mspt,
                        ping,
                        currentDimension,
                        new LinkedHashMap<>(tickMeridianTimes),
                        new LinkedHashMap<>(tickEntityTimes),
                        new LinkedHashMap<>(tickEntityCounts),
                        new LinkedHashMap<>(tickBlockEntityTimes),
                        new LinkedHashMap<>(tickBlockEntityCounts),
                        new LinkedHashMap<>(tickChunkSourceTimes),
                        new LinkedHashMap<>(tickChunkSourceCounts),
                        new LinkedHashMap<>(tickBlockEventTimes),
                        new LinkedHashMap<>(tickBlockEventCounts));
                if (worstTicks.size() >= WORST_TICK_LIMIT) worstTicks.poll();
                worstTicks.add(snapshot);
            }
        }

        tickMeridianTimes.clear();
        tickEntityTimes.clear();
        tickEntityCounts.clear();
        tickBlockEntityTimes.clear();
        tickBlockEntityCounts.clear();
        tickChunkSourceTimes.clear();
        tickChunkSourceCounts.clear();
        tickBlockEventTimes.clear();
        tickBlockEventCounts.clear();

        tickCount++;
    }

    private static long sumOf(Map<String, Long> map) {
        long total = 0;
        for (long v : map.values()) total += v;
        return total;
    }

    private void recordHistogram(long nanos) {
        double ms = nanos / 1_000_000.0;
        for (int i = 0; i < HIST_EDGES_MS.length; i++) {
            if (ms <= HIST_EDGES_MS[i]) {
                histogram[i]++;
                return;
            }
        }
        histogram[HIST_EDGES_MS.length - 1]++;
    }

    private int calcAveragePing() {
        if (pingCountdown-- > 0) {
            return cachedPing;
        }
        pingCountdown = PING_SAMPLE_INTERVAL_TICKS;
        int total = 0;
        int count = 0;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            total += p.getPing();
            count++;
        }
        cachedPing = count > 0 ? total / count : 0;
        return cachedPing;
    }

    /**
     * Bucket this tick into the wall-clock second it finished in. A second with no ticks at all
     * (a long freeze) still gets an entry, so the series cannot silently hide a gap.
     */
    private void countTickIntoSecond(long now) {
        if (secondStartMs == 0L) {
            secondStartMs = now;
        } else {
            while (now - secondStartMs >= 1000L) {
                ticksPerSecond.add(ticksThisSecond);
                ticksThisSecond = 0;
                secondStartMs += 1000L;
            }
        }
        ticksThisSecond++;
    }

    /** Sets the dimension used as a prefix by the following feelPulse calls. */
    public void setLevel(String dimension) {
        currentDimension = dimension;
    }

    public void feelPulse(String meridian) {
        if (currentMeridian != null) {
            pulseComplete();
        }
        currentMeridian = currentDimension != null ? currentDimension + ":" + meridian : meridian;
        meridianStartNs = System.nanoTime();
    }

    public void pulseComplete() {
        if (currentMeridian == null) return;
        long elapsed = System.nanoTime() - meridianStartNs;
        meridianTimes.merge(currentMeridian, elapsed, Long::sum);
        meridianCounts.merge(currentMeridian, 1, Integer::sum);
        tickMeridianTimes.merge(currentMeridian, elapsed, Long::sum);
        currentMeridian = null;
    }

    public void recordEntityPulse(String entityType, long nanos, UUID uuid, String world, int x, int y, int z) {
        String key = currentDimension != null ? currentDimension + "@@" + entityType : entityType;
        entityMeridianTimes.merge(key, nanos, Long::sum);
        entityMeridianCounts.merge(key, 1, Integer::sum);
        tickEntityTimes.merge(key, nanos, Long::sum);
        tickEntityCounts.merge(key, 1, Integer::sum);
        EntityTrace entityTrace = entityInstanceTraces
                .computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(uuid, k -> new EntityTrace(uuid, world, x, y, z));
        entityTrace.updatePosition(world, x, y, z);
        entityTrace.accumulate(nanos);
    }

    public void recordBlockEntityPulse(String blockEntityType, long nanos, String world, int x, int y, int z) {
        String key = currentDimension != null ? currentDimension + ":" + blockEntityType : blockEntityType;
        blockEntityMeridianTimes.merge(key, nanos, Long::sum);
        blockEntityMeridianCounts.merge(key, 1, Integer::sum);
        tickBlockEntityTimes.merge(key, nanos, Long::sum);
        tickBlockEntityCounts.merge(key, 1, Integer::sum);
        String posKey = world + ":" + x + "," + y + "," + z;
        blockEntityInstanceTraces
                .computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(posKey, k -> new BlockEntityTrace(blockEntityType, world, x, y, z))
                .accumulate(nanos);
    }

    public void recordChunkStat(int totalChunks, int activeChunks) {
        if (currentDimension == null) return;
        chunkStats.computeIfAbsent(currentDimension, k -> new ChunkStat()).accumulate(totalChunks, activeChunks);
    }

    /**
     * One movement sample for a player, taken once per tick. Distance and the chunk-crossing trail
     * are only kept for the chunk the player is standing in, because that is what drives chunk
     * loading — sub-block movement would just bloat the report.
     */
    public void recordPlayerMove(UUID uuid, String name, String dimension, double x, double y, double z) {
        playerTrails.computeIfAbsent(uuid, k -> new PlayerTrail(name))
                .observe(dimension, x, y, z, tickCount);
    }

    /**
     * A chunk that just gained a PLAYER ticket, i.e. the server started loading it because a player
     * came near. This is the "chunks loaded by player movement" record.
     */
    public void recordChunkTicket(long chunkPos) {
        String dimension = currentDimension != null ? currentDimension : "";
        ticketedChunks.computeIfAbsent(dimension, k -> new LongOpenHashSet()).add(chunkPos);
    }

    /** One sub-step of the chunk source tick, e.g. {@code purge} or {@code chunks}. */
    public void recordChunkSourcePulse(String step, long nanos) {
        String key = currentDimension != null ? currentDimension + ":" + step : step;
        chunkSourceTimes.merge(key, nanos, Long::sum);
        chunkSourceCounts.merge(key, 1, Integer::sum);
        tickChunkSourceTimes.merge(key, nanos, Long::sum);
        tickChunkSourceCounts.merge(key, 1, Integer::sum);
    }

    /** One processed block event, attributed to the block type that raised it. */
    public void recordBlockEventPulse(String blockType, long nanos) {
        String key = currentDimension != null ? currentDimension + ":" + blockType : blockType;
        blockEventTimes.merge(key, nanos, Long::sum);
        blockEventCounts.merge(key, 1, Integer::sum);
        tickBlockEventTimes.merge(key, nanos, Long::sum);
        tickBlockEventCounts.merge(key, 1, Integer::sum);
    }

    JsonObject toJson(long totalDurationMs) {
        JsonObject root = new JsonObject();
        root.addProperty("graspedTicks", tickCount);
        root.addProperty("avgTickTimeMs", tickCount > 0 ? String.format("%.2f", (double) totalDurationMs / tickCount) : "0");

        root.add("tickStats", buildTickStats(totalDurationMs));
        root.add("worstTicks", buildWorstTicksJson());
        root.add("meridians", buildSortedJsonArray(meridianTimes, meridianCounts, totalDurationMs));

        @SuppressWarnings("unchecked")
        Map<String, Map<Object, ? extends TraceData>> entityTraces = (Map<String, Map<Object, ? extends TraceData>>) (Map<?, ?>) entityInstanceTraces;
        root.add("entityMeridians", buildEntityJsonArray(entityMeridianTimes, entityMeridianCounts, entityTraces, totalDurationMs,
                (obj, trace, typeName) -> {
                    obj.addProperty("uuid", ((EntityTrace) trace).uuid.toString());
                    obj.addProperty("type", typeName);
                }));

        @SuppressWarnings("unchecked")
        Map<String, Map<Object, ? extends TraceData>> blockEntityTraces = (Map<String, Map<Object, ? extends TraceData>>) (Map<?, ?>) blockEntityInstanceTraces;
        root.add("blockEntityMeridians", buildEntityJsonArray(blockEntityMeridianTimes, blockEntityMeridianCounts, blockEntityTraces, totalDurationMs,
                (obj, trace, typeName) -> {
                    obj.addProperty("type", ((BlockEntityTrace) trace).type);
                    obj.addProperty("world", trace.world());
                }));

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
        root.add("worldChunks", buildChunkJsonArray());
        root.add("playerTrails", buildPlayerTrailsJson(totalDurationMs));
        root.add("chunkLoads", buildChunkLoadsJson());

        return root;
    }

    private JsonObject buildTickStats(long totalDurationMs) {
        JsonObject o = new JsonObject();
        o.addProperty("samples", tickCount);
        o.addProperty("measuredAvgMs", fmtMs(tickCount > 0 ? (double) tickNanosSum / tickCount : 0));
        o.addProperty("measuredMinMs", fmtMs(tickCount > 0 && tickNanosMin != Long.MAX_VALUE ? tickNanosMin : 0));
        o.addProperty("measuredMaxMs", fmtMs(tickNanosMax));
        o.addProperty("slowTicks", slowTickCount);
        o.addProperty("slowTickThresholdMs", SLOW_TICK_THRESHOLD_NANOS / 1_000_000);
        o.addProperty("slowTickPercent", tickCount > 0 ? String.format("%.2f", slowTickCount * 100.0 / tickCount) : "0.00");

        // The exact TPS of the whole window, from our own tick count — unlike the server's rolling
        // average it is not diluted by anything that happened before the run started.
        o.addProperty("windowTps", totalDurationMs > 0
                ? String.format("%.2f", tickCount * 1000.0 / totalDurationMs) : "0.00");
        JsonArray perSecond = new JsonArray();
        for (int ticks : ticksPerSecond) {
            perSecond.add(ticks);
        }
        o.addProperty("perSecondSamples", perSecond.size());
        o.add("tpsPerSecond", perSecond);

        // The tick rate can be lowered at runtime, and then a low TPS is by design — recording it
        // stops the report sending someone hunting for a fault that does not exist.
        o.addProperty("tickRate", String.format("%.1f", tickRate));
        o.addProperty("frozen", frozen);
        o.addProperty("sprinting", sprinting);

        // Where the second went: TPS x MSPT is the time actually spent inside ticks. The rest is a
        // wall-clock hole that tickBudget cannot see, because it only measures the inside of a tick.
        double mspt = tickCount > 0 ? (double) tickNanosSum / tickCount / 1_000_000.0 : 0;
        double actualTps = totalDurationMs > 0 ? tickCount * 1000.0 / totalDurationMs : 0;
        double tickMs = actualTps * mspt;
        double expectedTps = Math.min(tickRate, mspt > 0 ? 1000.0 / mspt : tickRate);
        o.addProperty("tickOccupancyPercent", String.format("%.2f", tickMs / 10.0));
        o.addProperty("expectedTps", String.format("%.2f", expectedTps));
        o.addProperty("holeMsPerSecond", String.format("%.2f", Math.max(0, 1000.0 - tickMs)));
        o.addProperty("occupancyVerdict", occupancyVerdict(actualTps, expectedTps, mspt));

        // The measured half of the hole: time the main thread spent waiting for async work, which
        // happens between ticks and therefore never shows up in MSPT.
        o.addProperty("asyncWaitSamples", asyncWaitCount);
        o.addProperty("asyncWaitAvgMs", asyncWaitCount > 0
                ? String.format("%.3f", asyncWaitNanosSum / 1_000_000.0 / asyncWaitCount) : "0.000");
        o.addProperty("asyncWaitMaxMs", String.format("%.3f", asyncWaitNanosMax / 1_000_000.0));
        o.addProperty("asyncWaitShareOfWallTimePercent", totalDurationMs > 0
                ? String.format("%.2f", asyncWaitNanosSum / 1_000_000.0 / totalDurationMs * 100) : "0.00");

        int n = Math.min(tickRingCount, TICK_RING_SIZE);
        if (n > 0) {
            long[] copy = new long[n];
            System.arraycopy(tickRing, 0, copy, 0, n);
            Arrays.sort(copy);
            o.addProperty("windowSamples", n);
            o.addProperty("p50Ms", fmtMs(copy[percentileIndex(n, 0.50)]));
            o.addProperty("p95Ms", fmtMs(copy[percentileIndex(n, 0.95)]));
            o.addProperty("p99Ms", fmtMs(copy[percentileIndex(n, 0.99)]));
        }

        JsonArray hist = new JsonArray();
        long lower = 0;
        for (int i = 0; i < HIST_EDGES_MS.length; i++) {
            long upper = HIST_EDGES_MS[i];
            JsonObject b = new JsonObject();
            b.addProperty("range", upper == Long.MAX_VALUE ? (">" + lower + "ms") : (lower + "-" + upper + "ms"));
            b.addProperty("count", histogram[i]);
            hist.add(b);
            lower = upper;
        }
        o.add("histogram", hist);
        return o;
    }

    private static int percentileIndex(int n, double p) {
        int idx = (int) Math.ceil(n * p) - 1;
        if (idx < 0) idx = 0;
        if (idx >= n) idx = n - 1;
        return idx;
    }

    private JsonArray buildWorstTicksJson() {
        JsonArray array = new JsonArray();
        List<SlowTick> sorted = new ArrayList<>(worstTicks);
        sorted.sort((a, b) -> Long.compare(b.durationNanos, a.durationNanos));
        for (SlowTick t : sorted) {
            JsonObject obj = new JsonObject();
            obj.addProperty("tick", t.tick);
            obj.addProperty("timestamp", t.timestampMs);
            obj.addProperty("tickMs", fmtMs(t.durationNanos));
            obj.addProperty("tps", t.tps);
            obj.addProperty("mspt", t.mspt);
            obj.addProperty("ping", t.ping);
            obj.addProperty("dimension", t.dimension != null ? t.dimension : "");
            obj.add("phases", buildTimingBreakdown(t.phases, null, t.durationNanos));
            obj.add("entityTypes", buildTimingBreakdown(t.entityTimes, t.entityCounts, t.durationNanos));
            obj.add("blockEntityTypes", buildTimingBreakdown(t.blockEntityTimes, t.blockEntityCounts, t.durationNanos));
            // the additive view of this very tick: segments + unaccounted == tickMs
            obj.add("budget", buildBudget(t.durationNanos, 1, t.phases, null,
                    families(t.entityTimes, t.entityCounts, t.blockEntityTimes, t.blockEntityCounts,
                            t.chunkSourceTimes, t.chunkSourceCounts, t.blockEventTimes, t.blockEventCounts)));
            array.add(obj);
        }
        return array;
    }

    /** Sorted [{name, ms, count, percent}] built from a timing map. */
    private JsonArray buildTimingBreakdown(Map<String, Long> times, Map<String, Integer> counts, long tickNanos) {
        JsonArray array = new JsonArray();
        if (times == null || times.isEmpty()) return array;
        List<Map.Entry<String, Long>> sorted = times.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .toList();
        for (Map.Entry<String, Long> e : sorted) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", e.getKey());
            obj.addProperty("ms", fmtMs(e.getValue()));
            if (counts != null) {
                obj.addProperty("count", counts.getOrDefault(e.getKey(), 0));
            }
            obj.addProperty("percent", tickNanos > 0
                    ? String.format("%.2f", e.getValue() * 100.0 / tickNanos) : "0.00");
            array.add(obj);
        }
        return array;
    }

    private static String fmtMs(double nanos) {
        return String.format("%.3f", nanos / 1_000_000.0);
    }

    // ---- tick time budget ----

    /** Phase whose window contains the per-entity timings. */
    private static final String ENTITY_PHASE = "entities";
    /** Phase whose window contains the per-block-entity timings. */
    private static final String BLOCK_ENTITY_PHASE = "blockEntities";
    /** Phase whose window contains the chunk source sub-steps. */
    private static final String CHUNK_SOURCE_PHASE = "chunkSource";
    /** Phase whose window contains the block event processing. */
    private static final String BLOCK_EVENT_PHASE = "blockEvents";
    /** Remainder bucket, always emitted last so the segments add up to the whole tick. */
    private static final String UNACCOUNTED = "unaccounted";
    /**
     * Phases measured by MinecraftServer itself, mirroring the feelPulse calls in the patches.
     * They are not tied to a dimension, but the recorder keeps the last-ticked dimension around,
     * so their keys can carry a meaningless prefix — which is dropped here.
     */
    private static final Set<String> SERVER_PHASES = Set.of(
            "tallying", "commandFunctions", "levels", "connection", "players", "send_chunks");

    /** One measured entity / block-entity / sub-step row, before it is attached to its phase. */
    private record TypeRow(String key, String label, long nanos, int count) {
    }

    /**
     * One drill-down family: the phase its rows belong to, the separator the recorder joins a
     * dimension prefix with, and the accumulated times / counts.
     */
    record Family(String phase, String separator, Map<String, Long> times, Map<String, Integer> counts) {
    }

    /** A phase after normalisation: what to call it, and the total across its raw keys. */
    private record PhaseAgg(String key, String name, String label, String dimension, long nanos, int count) {
    }

    /**
     * Normalise the recorded phases before they become segments.
     *
     * <p>The recorder prefixes a phase with whatever dimension ticked last, and the server-level
     * phases are measured in {@code MinecraftServer} — so they pick up a stale prefix and the same
     * phase ends up under two keys ({@code send_chunks} and {@code minecraft:the_nether:send_chunks}).
     * Those phases are declared in {@link #SERVER_PHASES}, so their dimension is dropped and the two
     * entries merge back into one segment instead of showing up twice.
     */
    private static List<PhaseAgg> mergePhases(Map<String, Long> phases, Map<String, Integer> counts) {
        Map<String, PhaseAgg> merged = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : phases.entrySet()) {
            String phase = labelOf(e.getKey());
            String dimension = dimensionOf(e.getKey(), phase);
            String shortDimension = shortDimension(dimension);
            String key = dimension + '\u0000' + phase;
            int count = counts == null ? 0 : counts.getOrDefault(e.getKey(), 0);
            merged.merge(key,
                    new PhaseAgg(key,
                            dimension.isEmpty() ? phase : dimension + ":" + phase,
                            shortDimension.isEmpty() ? phase : shortDimension + ":" + phase,
                            shortDimension, e.getValue(), count),
                    (a, b) -> new PhaseAgg(a.key(), a.name(), a.label(), a.dimension(),
                            a.nanos() + b.nanos(), a.count() + b.count()));
        }
        return merged.values().stream()
                .sorted((a, b) -> Long.compare(b.nanos(), a.nanos()))
                .toList();
    }

    /** The four families a budget can drill into, for one tick or a whole session. */
    private static List<Family> families(Map<String, Long> entityTimes, Map<String, Integer> entityCounts,
            Map<String, Long> blockEntityTimes, Map<String, Integer> blockEntityCounts,
            Map<String, Long> chunkSourceTimes, Map<String, Integer> chunkSourceCounts,
            Map<String, Long> blockEventTimes, Map<String, Integer> blockEventCounts) {
        return List.of(
                new Family(ENTITY_PHASE, "@@", entityTimes, entityCounts),
                new Family(BLOCK_ENTITY_PHASE, ":", blockEntityTimes, blockEntityCounts),
                new Family(CHUNK_SOURCE_PHASE, ":", chunkSourceTimes, chunkSourceCounts),
                new Family(BLOCK_EVENT_PHASE, ":", blockEventTimes, blockEventCounts));
    }

    /**
     * The composition of a tick: every measured phase with its drill-down, plus an
     * {@code unaccounted} remainder. Phases are exclusive segments, so the segments add up
     * to the tick they describe — which is what makes "the tick cost 50ms, where did it go?"
     * answerable. Drill-down rows are a <em>subset</em> of their phase, not siblings of it.
     *
     * @param tickNanos the tick time the budget has to add up to
     * @param divisor   1 for a single tick, or the tick count to average a whole session
     */
    static JsonObject buildBudget(long tickNanos, int divisor, Map<String, Long> phases,
            Map<String, Integer> phaseCounts, List<Family> families) {

        double scale = divisor > 0 ? divisor : 1;
        double tickMs = tickNanos / 1_000_000.0 / scale;

        Map<String, List<TypeRow>> drilldown = new LinkedHashMap<>();
        for (Family family : families) {
            collectTypes(drilldown, phases, family.times(), family.counts(), family.phase(), family.separator());
        }

        double accountedMs = 0;
        JsonArray segments = new JsonArray();
        for (PhaseAgg phase : mergePhases(phases, phaseCounts)) {
            double ms = phase.nanos() / 1_000_000.0 / scale;
            accountedMs += ms;
            JsonArray children = new JsonArray();
            List<TypeRow> rows = drilldown.get(phase.key());
            if (rows != null) {
                rows.stream()
                        .sorted((a, b) -> Long.compare(b.nanos(), a.nanos()))
                        .forEach(row -> children.add(budgetRow(row.key(), "", row.label(),
                                row.nanos() / 1_000_000.0 / scale, tickMs, ms, row.count(), new JsonArray())));
            }
            segments.add(budgetRow(phase.name(), phase.dimension(), phase.label(),
                    ms, tickMs, tickMs, phase.count(), children));
        }

        double unaccountedMs = Math.max(0, tickMs - accountedMs);
        segments.add(budgetRow(UNACCOUNTED, "", UNACCOUNTED, unaccountedMs, tickMs, tickMs, 0, new JsonArray()));

        JsonObject budget = new JsonObject();
        budget.addProperty("samples", Math.max(1, divisor));
        budget.addProperty("tickMs", ms(tickMs));
        budget.addProperty("accountedMs", ms(accountedMs));
        budget.addProperty("unaccountedMs", ms(unaccountedMs));
        budget.add("segments", segments);
        return budget;
    }

    /**
     * Record how long the main thread waited for async work before the next tick could start. This
     * sits outside the tick, so it is the one part of the wall-clock hole tickBudget cannot see.
     */
    public void recordAsyncWait(long nanos) {
        if (nanos <= 0) {
            return;
        }
        asyncWaitNanosSum += nanos;
        if (nanos > asyncWaitNanosMax) {
            asyncWaitNanosMax = nanos;
        }
        asyncWaitCount++;
    }

    /** The session-wide budget: the composition of an average tick. */
    JsonObject sessionBudget() {
        return buildBudget(tickNanosSum, tickCount, meridianTimes, meridianCounts,
                families(entityMeridianTimes, entityMeridianCounts,
                        blockEntityMeridianTimes, blockEntityMeridianCounts,
                        chunkSourceTimes, chunkSourceCounts,
                        blockEventTimes, blockEventCounts));
    }

    private static JsonObject budgetRow(String name, String dimension, String label,
            double ms, double tickMs, double parentMs, int count, JsonArray children) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("dimension", dimension);
        obj.addProperty("label", label);
        obj.addProperty("ms", ms(ms));
        obj.addProperty("percent", percent(ms, tickMs));
        obj.addProperty("percentOfParent", percent(ms, parentMs));
        obj.addProperty("count", count);
        obj.add("children", children);
        return obj;
    }

    /** The most specific part of a phase key, for chart labels: "a:b:c" -> "c". */
    private static String labelOf(String key) {
        int colon = key.lastIndexOf(':');
        return colon >= 0 ? key.substring(colon + 1) : key;
    }

    /** "minecraft:overworld" -> "overworld"; anything without a namespace is kept as-is. */
    private static String shortDimension(String dimension) {
        int colon = dimension.indexOf(':');
        return colon >= 0 ? dimension.substring(colon + 1) : dimension;
    }

    private static String ms(double millis) {
        return String.format("%.3f", millis);
    }

    private static String percent(double part, double whole) {
        return whole > 0 ? String.format("%.2f", part / whole * 100) : "0.00";
    }

    /**
     * Attach every recorded type to the phase whose window it was recorded in. The phase keys
     * themselves are the ground truth for the dimension prefix, so no key format is assumed.
     */
    private static void collectTypes(Map<String, List<TypeRow>> out, Map<String, Long> phases,
            Map<String, Long> times, Map<String, Integer> counts, String phase, String separator) {
        if (times.isEmpty()) return;
        Map<String, String> phaseKeyByDimension = new LinkedHashMap<>();
        for (String key : phases.keySet()) {
            if (key.equals(phase)) {
                phaseKeyByDimension.put("", key);
            } else if (key.endsWith(":" + phase)) {
                phaseKeyByDimension.put(key.substring(0, key.length() - phase.length() - 1), key);
            }
        }
        for (Map.Entry<String, Long> e : times.entrySet()) {
            String parentKey = parentPhase(e.getKey(), separator, phaseKeyByDimension, phase);
            String dimension = dimensionOf(parentKey, phase);
            // drop the dimension prefix so a block entity reads "minecraft:hopper", not just "hopper"
            String label = dimension.isEmpty() ? e.getKey()
                    : e.getKey().substring(dimension.length() + separator.length());
            // key by the same normalised form the segments use, so the drill-down lines up
            out.computeIfAbsent(dimension + '\u0000' + phase, k -> new ArrayList<>())
                    .add(new TypeRow(e.getKey(), label, e.getValue(),
                            counts == null ? 0 : counts.getOrDefault(e.getKey(), 0)));
        }
    }

    /** "minecraft:overworld:entities" -> "minecraft:overworld"; a bare or server-level phase -> "". */
    private static String dimensionOf(String phaseKey, String phase) {
        if (phaseKey.equals(phase) || SERVER_PHASES.contains(phase)) {
            return "";
        }
        return phaseKey.substring(0, phaseKey.length() - phase.length() - 1);
    }

    /** The phase key a type belongs to: the longest matching dimension prefix wins. */
    private static String parentPhase(String typeKey, String separator,
            Map<String, String> phaseKeyByDimension, String fallback) {
        String match = null;
        int matchLength = -1;
        String bare = null;
        for (Map.Entry<String, String> e : phaseKeyByDimension.entrySet()) {
            String dimension = e.getKey();
            if (dimension.isEmpty()) {
                bare = e.getValue();
            } else if (typeKey.startsWith(dimension + separator) && dimension.length() > matchLength) {
                matchLength = dimension.length();
                match = e.getValue();
            }
        }
        if (match != null) return match;
        return bare != null ? bare : fallback;
    }

    /**
     * Says which way to look, so nobody has to remember the arithmetic:
     *
     * <ul>
     *   <li>{@code FROZEN} — ticking is paused, TPS is meaningless</li>
     *   <li>{@code RATE_LIMITED} — {@code /tick rate} lowered the rate and the server is keeping up
     *       with it; the low TPS is by design</li>
     *   <li>{@code TICK_BOUND} — actual TPS matches what the tick work allows and the budget is
     *       nearly used up, so the cost is inside the tick → read tickBudget</li>
     *   <li>{@code WALL_CLOCK_HOLE} — TPS is well below what the tick work allows, so time is being
     *       lost between ticks → tickBudget cannot see it, look at GC / CPU / async workers</li>
     *   <li>{@code HEALTHY} — matches expectations with headroom left</li>
     * </ul>
     */
    private String occupancyVerdict(double actualTps, double expectedTps, double mspt) {
        if (frozen) {
            return "FROZEN";
        }
        if (tickRate < 19.5F && actualTps >= tickRate * 0.95) {
            return "RATE_LIMITED";
        }
        if (expectedTps > 0 && actualTps < expectedTps * 0.95) {
            return "WALL_CLOCK_HOLE";
        }
        return mspt >= 45 ? "TICK_BOUND" : "HEALTHY";
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

    // ---- player movement ----

    /** Chunks within this radius of a player's trail are attributed to that player. */
    private static final int TRAIL_ATTRIBUTION_RADIUS = 12;
    /**
     * A player crossing this many chunks per minute is travelling faster than any normal
     * traversal (sprinting is ~21/min, a horse ~54/min, an elytra or a boat on ice 120+/min).
     */
    private static final double SUSPECT_CHUNKS_PER_MINUTE = 90;
    private static final double ELEVATED_CHUNKS_PER_MINUTE = 40;
    /** Blocks per second, for the same reason. */
    private static final double SUSPECT_SPEED = 20;
    private static final double ELEVATED_SPEED = 12;
    /** Below this the player was barely online during the run, so no verdict is given. */
    private static final int MIN_SAMPLES_TO_JUDGE = 20;
    /**
     * Upper bound on the chunk coordinates written into the report. A player teleporting across
     * unexplored terrain can ticket tens of thousands of chunks in one run, and the upload service
     * rejects oversized bodies — so past this the heat map is a sample and {@code total} is the
     * real figure.
     */
    private static final int MAX_TICKET_CHUNKS = 40_000;

    /** A point on a player's trail; one per chunk the player entered. */
    private record TrailPoint(int tick, long timestampMs, double x, double y, double z,
                              int chunkX, int chunkZ, String dimension) {
    }

    /** Movement and chunk footprint of one player during the run. */
    private static final class PlayerTrail {
        final String name;
        final List<TrailPoint> points = new ArrayList<>();
        /** Distinct chunks the player stood in — the trail, deduplicated. */
        final LongSet chunks = new LongOpenHashSet();
        double distance;
        /** Largest single-tick horizontal move, in blocks. */
        double maxStep;
        int samples;
        String dimension = "";
        private double lastX = Double.NaN;
        private double lastZ;
        private long lastChunk = Long.MIN_VALUE;

        PlayerTrail(String name) {
            this.name = name == null ? "unknown" : name;
        }

        void observe(String dim, double x, double y, double z, int tick) {
            samples++;
            if (!Double.isNaN(lastX) && dim.equals(dimension)) {
                double dx = x - lastX;
                double dz = z - lastZ;
                double step = Math.sqrt(dx * dx + dz * dz);
                distance += step;
                if (step > maxStep) maxStep = step;
            }
            long chunk = chunkKey(blockToChunk(x), blockToChunk(z));
            if (chunk != lastChunk || !dim.equals(dimension)) {
                lastChunk = chunk;
                points.add(new TrailPoint(tick, System.currentTimeMillis(), x, y, z,
                        chunkX(chunk), chunkZ(chunk), dim));
                chunks.add(chunk);
            }
            dimension = dim;
            lastX = x;
            lastZ = z;
        }
    }

    private static int blockToChunk(double coordinate) {
        return (int) Math.floor(coordinate) >> 4;
    }

    /**
     * Must match {@code ChunkPos.asLong} exactly, because the chunk positions arrive from
     * {@code DistanceManager} already packed that way. Note the order: <b>x is the low 32 bits and
     * z the high 32</b>, which is the opposite of the obvious packing. Getting it backwards swaps
     * the axes in the exported coordinates.
     */
    private static long chunkKey(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    private static int chunkX(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }

    private static int chunkZ(long key) {
        return (int) (key >>> 32 & 0xFFFFFFFFL);
    }

    private JsonArray buildPlayerTrailsJson(long durationMs) {
        double seconds = Math.max(1, durationMs) / 1000.0;
        Map<UUID, Integer> attributed = attributeTicketedChunks();

        List<Map.Entry<UUID, PlayerTrail>> sorted = new ArrayList<>(playerTrails.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue().distance, a.getValue().distance));

        JsonArray array = new JsonArray();
        for (Map.Entry<UUID, PlayerTrail> e : sorted) {
            PlayerTrail trail = e.getValue();
            double avgSpeed = trail.distance / seconds;
            double chunksPerMinute = trail.chunks.size() * 60000.0 / Math.max(1, durationMs);

            JsonObject o = new JsonObject();
            o.addProperty("uuid", e.getKey().toString());
            o.addProperty("name", trail.name);
            o.addProperty("dimension", trail.dimension);
            o.addProperty("samples", trail.samples);
            o.addProperty("distance", ms(trail.distance));
            o.addProperty("avgSpeed", ms(avgSpeed));
            o.addProperty("maxSpeed", ms(trail.maxStep * 20)); // one tick at this step = this many blocks/s
            o.addProperty("chunksEntered", trail.chunks.size());
            o.addProperty("chunksPerMinute", ms(chunksPerMinute));
            o.addProperty("chunksLoaded", attributed.getOrDefault(e.getKey(), 0));
            o.addProperty("verdict", verdictOf(trail, avgSpeed, chunksPerMinute));

            JsonArray points = new JsonArray();
            for (TrailPoint p : trail.points) {
                JsonObject point = new JsonObject();
                point.addProperty("tick", p.tick());
                point.addProperty("timestamp", p.timestampMs());
                point.addProperty("x", ms(p.x()));
                point.addProperty("y", ms(p.y()));
                point.addProperty("z", ms(p.z()));
                point.addProperty("cx", p.chunkX());
                point.addProperty("cz", p.chunkZ());
                point.addProperty("dimension", p.dimension());
                points.add(point);
            }
            o.add("trail", points);
            array.add(o);
        }
        return array;
    }

    private static String verdictOf(PlayerTrail trail, double avgSpeed, double chunksPerMinute) {
        if (trail.samples < MIN_SAMPLES_TO_JUDGE) {
            return "UNKNOWN";
        }
        if (chunksPerMinute >= SUSPECT_CHUNKS_PER_MINUTE || avgSpeed >= SUSPECT_SPEED) {
            return "SUSPECT";
        }
        if (chunksPerMinute >= ELEVATED_CHUNKS_PER_MINUTE || avgSpeed >= ELEVATED_SPEED) {
            return "ELEVATED";
        }
        return "NORMAL";
    }

    /**
     * Nearest-trail attribution: a chunk that gained a player ticket belongs to the player whose
     * trail passed closest to it. The trail is chunk-granular and players can be far apart, so the
     * nearest match is a good estimate rather than an exact answer.
     */
    private Map<UUID, Integer> attributeTicketedChunks() {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        if (ticketedChunks.isEmpty() || playerTrails.isEmpty()) {
            return counts;
        }

        Long2ObjectMap<UUID> owner = new Long2ObjectOpenHashMap<>();
        for (Map.Entry<UUID, PlayerTrail> e : playerTrails.entrySet()) {
            for (long chunk : e.getValue().chunks) {
                owner.putIfAbsent(chunk, e.getKey());
            }
        }

        int radius = TRAIL_ATTRIBUTION_RADIUS;
        for (Map.Entry<String, LongSet> byDimension : ticketedChunks.entrySet()) {
            String ticketDimension = byDimension.getKey();
            // a trail point only competes for chunks in its own dimension; an unknown dimension
            // (recorded before any level ticked) must not silently attribute nothing
            boolean anyDimension = ticketDimension.isEmpty();
            Long2ObjectMap<UUID> dimensionOwner = new Long2ObjectOpenHashMap<>();
            for (Map.Entry<UUID, PlayerTrail> e : playerTrails.entrySet()) {
                for (TrailPoint point : e.getValue().points) {
                    if (anyDimension || point.dimension().equals(ticketDimension)) {
                        dimensionOwner.putIfAbsent(chunkKey(point.chunkX(), point.chunkZ()), e.getKey());
                    }
                }
            }
            for (long chunk : byDimension.getValue()) {
                int cx = chunkX(chunk);
                int cz = chunkZ(chunk);
                UUID best = null;
                int bestDistance = Integer.MAX_VALUE;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        UUID candidate = dimensionOwner.get(chunkKey(cx + dx, cz + dz));
                        if (candidate == null) {
                            continue;
                        }
                        int distance = dx * dx + dz * dz;
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = candidate;
                        }
                    }
                }
                if (best != null) {
                    counts.merge(best, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private JsonObject buildChunkLoadsJson() {
        JsonObject o = new JsonObject();
        JsonArray dimensions = new JsonArray();
        int total = 0;
        int emitted = 0;
        for (Map.Entry<String, LongSet> entry : ticketedChunks.entrySet()) {
            total += entry.getValue().size();
            JsonArray coords = new JsonArray();
            for (long chunk : entry.getValue()) {
                if (emitted >= MAX_TICKET_CHUNKS) {
                    break;
                }
                JsonArray pair = new JsonArray();
                pair.add(chunkX(chunk));
                pair.add(chunkZ(chunk));
                coords.add(pair);
                emitted++;
            }
            JsonObject group = new JsonObject();
            group.addProperty("dimension", entry.getKey());
            group.addProperty("total", entry.getValue().size());
            group.addProperty("emitted", coords.size());
            group.add("chunks", coords);
            dimensions.add(group);
        }
        o.addProperty("total", total);
        o.addProperty("attributionRadius", TRAIL_ATTRIBUTION_RADIUS);
        o.addProperty("emitted", emitted);
        o.addProperty("truncated", total > emitted);
        o.add("dimensions", dimensions);
        return o;
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

            // emitted even when empty so the viewer schema stays fixed
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

    /** One slow tick, with everything that was running during it. */
    private static final class SlowTick {
        final int tick;
        final long timestampMs;
        final long durationNanos;
        final double tps;
        final double mspt;
        final int ping;
        final String dimension;
        final Map<String, Long> phases;
        final Map<String, Long> entityTimes;
        final Map<String, Integer> entityCounts;
        final Map<String, Long> blockEntityTimes;
        final Map<String, Integer> blockEntityCounts;
        final Map<String, Long> chunkSourceTimes;
        final Map<String, Integer> chunkSourceCounts;
        final Map<String, Long> blockEventTimes;
        final Map<String, Integer> blockEventCounts;

        SlowTick(int tick, long timestampMs, long durationNanos, double tps, double mspt, int ping, String dimension,
                 Map<String, Long> phases, Map<String, Long> entityTimes, Map<String, Integer> entityCounts,
                 Map<String, Long> blockEntityTimes, Map<String, Integer> blockEntityCounts,
                 Map<String, Long> chunkSourceTimes, Map<String, Integer> chunkSourceCounts,
                 Map<String, Long> blockEventTimes, Map<String, Integer> blockEventCounts) {
            this.tick = tick;
            this.timestampMs = timestampMs;
            this.durationNanos = durationNanos;
            this.tps = tps;
            this.mspt = mspt;
            this.ping = ping;
            this.dimension = dimension;
            this.phases = phases;
            this.entityTimes = entityTimes;
            this.entityCounts = entityCounts;
            this.blockEntityTimes = blockEntityTimes;
            this.blockEntityCounts = blockEntityCounts;
            this.chunkSourceTimes = chunkSourceTimes;
            this.chunkSourceCounts = chunkSourceCounts;
            this.blockEventTimes = blockEventTimes;
            this.blockEventCounts = blockEventCounts;
        }
    }
}
