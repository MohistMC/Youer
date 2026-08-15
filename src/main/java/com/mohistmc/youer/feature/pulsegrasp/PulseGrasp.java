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
 * PulseGrasp — server-side diagnostic system.
 * <p>
 * Samples per-tick timing, entity/block-entity timing, TPS/MSPT/Ping
 * vital-sign series, and packet send statistics. On stop, outputs a merged
 * JSON report to locate MSPT bottlenecks and network traffic patterns.
 * <p>
 * Merged data:
 * - Block entities hold both tick time (compute) and packet bytes (network)
 * - Players hold both Ping (latency) and packet bytes (traffic)
 * - Chunks expose packet volume to detect abnormal high-frequency chunks
 */
public class PulseGrasp {

    private static final PulseGrasp instance = new PulseGrasp();
    private volatile boolean grasping = false;
    private long graspStartMs;

    // Recorder (start/stop command executor)
    private String startName;
    private String startUuid;
    private String stopName;
    private String stopUuid;

    private final TickProfiler tickProfiler = new TickProfiler();
    private final PacketProfiler packetProfiler = new PacketProfiler();
    private final ThreadProfiler threadProfiler = new ThreadProfiler();
    private final SystemProfiler systemProfiler = new SystemProfiler();
    private final MethodSampler methodSampler = new MethodSampler();

    // Async diagnostic thread (report generated in background)
    private Thread diagnoseThread;
    private String lastReportPath;
    private String notifyUuid;

    // ---- Singleton ----

    public static PulseGrasp instance() {
        return instance;
    }

    public static boolean isGrasping() {
        return instance.grasping;
    }

    // ---- Lifecycle ----

    /** Optional sampling params; null fields fall back to MethodSampler defaults (25ms / 64 / 40). */
    public static final class SampleOptions {
        public final Long intervalMs;
        public final Integer maxDepth;
        public final Integer maxTopN;

        public SampleOptions(Long intervalMs, Integer maxDepth, Integer maxTopN) {
            this.intervalMs = intervalMs;
            this.maxDepth = maxDepth;
            this.maxTopN = maxTopN;
        }
    }

    /** Start — begins tick timing, packet analysis, and system sampling */
    public void startGrasp(String name, String uuid) {
        startGrasp(name, uuid, null);
    }

    /** Start — uses MethodSampler defaults when opts or fields are null */
    public void startGrasp(String name, String uuid, SampleOptions opts) {
        if (grasping) return;
        joinDiagnoseThread(); // wait for previous diagnostic thread to read clean data
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
        if (opts != null) {
            if (opts.intervalMs != null) methodSampler.setIntervalMs(opts.intervalMs);
            if (opts.maxDepth != null) methodSampler.setMaxDepth(opts.maxDepth);
            if (opts.maxTopN != null) methodSampler.setMaxTopN(opts.maxTopN);
        }
        methodSampler.start(serverThreadId());
    }

    /** Stop — generates the report on a background thread, returns immediately */
    public void stopGraspAndDiagnose(String name, String uuid) {
        if (!grasping) return;
        stopName = name;
        stopUuid = uuid;
        notifyUuid = uuid;
        grasping = false;
        packetProfiler.stop();
        systemProfiler.stop();
        methodSampler.stop();
        diagnoseThread = new Thread(() -> {
            diagnose();
            notifyReportDone();
        }, "PulseGrasp-Diagnose");
        diagnoseThread.setDaemon(true); // don't block server shutdown
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

    /** Resolve the Server thread ID for stack sampling; fall back to current thread */
    private long serverThreadId() {
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        Thread serverThread = server != null ? server.getRunningThread() : null;
        return serverThread != null ? serverThread.getId() : Thread.currentThread().getId();
    }

    /** Notify on report completion — console log + notify the requesting player */
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
                // keep console log only on invalid UUID
            }
        });
    }

    // ============ Static convenience methods ============

    public static void start() {
        instance.startGrasp("console", "none");
    }

    public static void start(String name, String uuid) {
        instance.startGrasp(name, uuid);
    }

    public static void start(String name, String uuid, SampleOptions opts) {
        instance.startGrasp(name, uuid, opts);
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

    // ---- Internal delegation (no exposed instance()) ----

    public TickProfiler tickProfiler() {
        return tickProfiler;
    }

    public PacketProfiler packetProfiler() {
        return packetProfiler;
    }

    // ---- Diagnostic report ----

    private void diagnose() {
        long durationMs = System.currentTimeMillis() - graspStartMs;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path path = Paths.get("pulse_grasp_" + timestamp + ".json");
        lastReportPath = path.toString();

        JsonObject root = new JsonObject();
        root.addProperty("system", "PulseGrasp");
        root.addProperty("durationMs", durationMs);
        root.addProperty("durationSeconds", durationMs / 1000);

        // ====== Recorder info ======
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

        // ====== Layer 1: Tick timing data ======
        JsonObject tickData = tickProfiler.toJson(durationMs);
        // flatten tick sub-fields to root
        copyProperty(tickData, root, "graspedTicks");
        copyProperty(tickData, root, "avgTickTimeMs");
        copyProperty(tickData, root, "meridians");
        copyProperty(tickData, root, "entityMeridians");
        copyProperty(tickData, root, "vitalSigns");
        copyProperty(tickData, root, "worldChunks");

        // ====== Layer 2: Packet data (called once, shared by merge and network section) ======
        JsonObject packetData = packetProfiler.toJson(durationMs);

        // ====== Block entities: merge tick time + packet data ======
        // tick data provides blockEntityMeridians (time + topConsumers)
        // packet data provides blockEntityStats (bytes)
        // merge: inject packetBytes/packetCount into each type entry
        JsonObject beMerged = mergeBlockEntityData(
                tickData.getAsJsonArray("blockEntityMeridians"),
                packetData.getAsJsonObject("blockEntityStats")
        );
        root.add("blockEntityMeridians", beMerged);

        // ====== Network packet data ======
        JsonObject networkSection = new JsonObject();
        networkSection.addProperty("totalBytes", packetData.get("totalBytes").getAsLong());
        networkSection.addProperty("totalPackets", packetData.get("totalPackets").getAsLong());
        networkSection.addProperty("bytesPerSecond", packetData.get("bytesPerSecond").getAsLong());
        networkSection.addProperty("packetsPerSecond", packetData.get("packetsPerSecond").getAsLong());
        networkSection.add("packetTypes", packetData.get("packetTypes"));
        networkSection.add("playerStats", packetData.get("playerStats"));
        networkSection.add("chunkStats", packetData.get("chunkStats"));
        networkSection.add("flowSeries", packetData.get("flowSeries"));
        // block entity packet data already merged into blockEntityMeridians
        root.add("network", networkSection);

        // ====== Layer 3: Thread CPU snapshot (at stop) ======
        root.add("threadDump", threadProfiler.capture());

        // ====== Method-level self-time sampling (Server thread stack samples) ======
        root.add("methodSampler", methodSampler.toJson());

        // ====== System resource time series (memory + CPU) ======
        root.add("systemSeries", systemProfiler.toJson());

        // write to file
        try (FileWriter writer = new FileWriter(path.toFile())) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Merge block entity data: combine tick time (blockEntityMeridians JsonArray) with packet stats (blockEntityStats JsonObject).
     * Each type entry gets both tick performance and network traffic metrics.
     */
    private JsonObject mergeBlockEntityData(JsonArray tickBeArray, JsonObject packetBeStats) {
        JsonObject merged = new JsonObject();

        // convert tick JsonArray to name-indexed Map
        java.util.Map<String, JsonObject> tickLookup = new java.util.LinkedHashMap<>();
        if (tickBeArray != null) {
            for (int i = 0; i < tickBeArray.size(); i++) {
                JsonObject entry = tickBeArray.get(i).getAsJsonObject();
                tickLookup.put(entry.get("name").getAsString(), entry);
            }
        }

        // convert packet types to name-indexed Map
        java.util.Map<String, JsonObject> packetLookup = new java.util.LinkedHashMap<>();
        if (packetBeStats != null && packetBeStats.has("types")) {
            JsonObject packetTypes = packetBeStats.getAsJsonObject("types");
            for (String key : packetTypes.keySet()) {
                packetLookup.put(key, packetTypes.getAsJsonObject(key));
            }
        }

        // collect all type names (union of tick and packet)
        java.util.Set<String> allTypes = new java.util.LinkedHashSet<>();
        allTypes.addAll(tickLookup.keySet());
        allTypes.addAll(packetLookup.keySet());

        JsonObject mergedTypes = new JsonObject();

        for (String type : allTypes) {
            JsonObject entry = new JsonObject();

            // tick data
            JsonObject tickEntry = tickLookup.get(type);
            if (tickEntry != null) {
                copyProperty(tickEntry, entry, "totalMs");
                copyProperty(tickEntry, entry, "count");
                copyProperty(tickEntry, entry, "avgMs");
                copyProperty(tickEntry, entry, "percentage");
                copyProperty(tickEntry, entry, "topConsumers");
            }

            // packet data
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

        // sort by tick total time descending (tick entries first), then output as JsonArray
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
                    // no tick data for either; sort by packetBytes descending
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

        // position-level data (keep packet stats only)
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