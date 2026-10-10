package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohistmc.youer.api.ChatComponentAPI;
import com.mohistmc.youer.util.I18n;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Bukkit;

/**
 * PulseGrasp — server-side diagnostic system: samples tick / entity / block-entity timing,
 * TPS-MSPT-Ping vitals and packet traffic, then writes a merged JSON report and uploads it.
 * A run is always {@link #GRASP_DURATION_MS} long and ends on its own — there is no manual stop.
 */
public class PulseGrasp {

    /** Every run lasts exactly this long. */
    public static final long GRASP_DURATION_MS = 30_000L;
    /**
     * Runs have to be spaced out because the report service accepts one upload per minute per IP.
     * A 30s run uploads at t+30, so a 60s gap between starts puts uploads exactly 60s apart.
     */
    public static final long START_COOLDOWN_MS = 60_000L;
    /** Action-bar refresh period, and the countdown resolution. */
    private static final long PROGRESS_INTERVAL_MS = 1_000L;
    private static final int PROGRESS_BAR_LENGTH = 10;
    /** Counting active chunks walks every chunk, so it is sampled rather than measured each tick. */
    private static final int CHUNK_STAT_INTERVAL_TICKS = 5;

    private static final PulseGrasp instance = new PulseGrasp();

    private volatile boolean grasping = false;
    private long graspStartMs;
    private long lastStartMs;

    // Who ran /pulsegrasp start; the run ends by itself, so there is no separate stopper.
    private String startName;
    private String startUuid;

    private final TickProfiler tickProfiler = new TickProfiler();
    private final PacketProfiler packetProfiler = new PacketProfiler();
    private final ThreadProfiler threadProfiler = new ThreadProfiler();
    private final SystemProfiler systemProfiler = new SystemProfiler();
    private final MethodSampler methodSampler = new MethodSampler();
    private final AsyncProfiler asyncProfiler = new AsyncProfiler();

    // Set only when a player started the run; console runs show no progress at all.
    private volatile String progressUuid;

    // Async report generation
    private Thread diagnoseThread;

    public static PulseGrasp instance() {
        return instance;
    }

    public static boolean isGrasping() {
        return instance.grasping;
    }

    /** Run length in whole seconds, for user-facing messages. */
    public static long durationSeconds() {
        return GRASP_DURATION_MS / 1000;
    }

    /** Seconds left before another run may be started; 0 when starting is allowed right now. */
    public static long cooldownSeconds() {
        return instance.cooldownRemainingSeconds();
    }

    /**
     * Start a fixed-length run. Returns false when a run is already in progress or the cooldown has
     * not elapsed yet, so the caller can say which of the two it was.
     * {@code uuid} is "none" for non-players, which disables progress.
     */
    public static boolean start(String name, String uuid) {
        return instance.startGrasp(name, uuid);
    }

    private synchronized boolean startGrasp(String name, String uuid) {
        if (grasping || cooldownRemainingSeconds() > 0) return false;
        joinDiagnoseThread(); // wait for the previous report to be read out
        grasping = true;
        graspStartMs = System.currentTimeMillis();
        lastStartMs = graspStartMs;
        startName = name;
        startUuid = uuid;
        progressUuid = isPlayer(uuid) ? uuid : null;
        tickProfiler.reset();
        packetProfiler.reset();
        packetProfiler.start();
        systemProfiler.reset();
        systemProfiler.start();
        methodSampler.start(serverThreadId());
        asyncProfiler.reset();
        asyncProfiler.start(Thread.currentThread().getName());

        Thread timer = new Thread(this::countdown, "PulseGrasp-Timer");
        timer.setDaemon(true);
        timer.start();
        return true;
    }

    private synchronized long cooldownRemainingSeconds() {
        long elapsed = System.currentTimeMillis() - lastStartMs;
        if (elapsed >= START_COOLDOWN_MS) {
            return 0;
        }
        return (START_COOLDOWN_MS - elapsed + 999) / 1000;
    }

    private static boolean isPlayer(String uuid) {
        return uuid != null && !"none".equals(uuid);
    }

    /** Ticks the countdown, refreshes the action bar, then ends the run when the time is up. */
    private void countdown() {
        long deadline = graspStartMs + GRASP_DURATION_MS;
        try {
            while (grasping) {
                showProgress();
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;
                Thread.sleep(Math.min(PROGRESS_INTERVAL_MS, remaining));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[PulseGrasp] progress timer failed: " + t);
        } finally {
            stopGraspAndDiagnose();
        }
    }

    /** Action-bar progress for the player who started the run; a no-op for console runs. */
    private void showProgress() {
        String uuid = progressUuid;
        if (uuid == null || !grasping) return;
        long elapsed = System.currentTimeMillis() - graspStartMs;
        long remainingSec = Math.max(0, (GRASP_DURATION_MS - elapsed + 999) / 1000);
        String bar = progressBar(elapsed);
        String ticks = String.valueOf(getTickCount());
        withPlayer(uuid, player -> player.sendActionBar(I18n.as("pulsegrasp.progress",
                bar, String.valueOf(remainingSec), ticks)));
    }

    private static String progressBar(long elapsedMs) {
        long filled = Math.max(0, Math.min(PROGRESS_BAR_LENGTH, elapsedMs * PROGRESS_BAR_LENGTH / GRASP_DURATION_MS));
        return "§a" + "■".repeat((int) filled) + "§7" + "□".repeat(PROGRESS_BAR_LENGTH - (int) filled);
    }

    /**
     * End the run: build the report on a background thread and return immediately.
     * Upload runs on its own thread because the next start joins the diagnose thread,
     * so a slow network must not be part of it.
     */
    private synchronized void stopGraspAndDiagnose() {
        if (!grasping) return;
        grasping = false;
        progressUuid = null;
        packetProfiler.stop();
        systemProfiler.stop();
        methodSampler.stop();
        asyncProfiler.stop();
        String requester = startUuid;
        diagnoseThread = new Thread(() -> {
            Report report = diagnose();
            Thread upload = new Thread(() -> publish(report, requester), "PulseGrasp-Upload");
            upload.setDaemon(true);
            upload.start();
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

    /** Server thread id for stack sampling; falls back to the caller. */
    private long serverThreadId() {
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        Thread serverThread = server != null ? server.getRunningThread() : null;
        return serverThread != null ? serverThread.getId() : Thread.currentThread().getId();
    }

    /**
     * Publish the report: upload it and hand the share link to the player. Only when the upload
     * does not go through (failed, or uploading is switched off) is the JSON written to disk, so
     * a normal run leaves no file behind on the server.
     */
    private void publish(Report report, String uuid) {
        if (report == null) {
            // the report never got built, so there is nothing to upload or save
            Bukkit.getLogger().warning(I18n.as("pulsegrasp.build.failed.log"));
            notifyPlayer(uuid, I18n.as("pulsegrasp.build.failed"));
            return;
        }

        String url = null;
        String error = null;
        if (PulseGraspUploader.isEnabled()) {
            try {
                url = PulseGraspUploader.upload(report.json());
            } catch (Throwable t) {
                error = PulseGraspUploader.describeFailure(t);
                // full detail (endpoint, payload size, stack trace) for the operator
                Bukkit.getLogger().warning("[PulseGrasp] upload failed: url=" + PulseGraspUploader.uploadUrl()
                        + " size=" + humanSize(report.json().getBytes(StandardCharsets.UTF_8).length) + " cause=" + t);
            }
        } else {
            Bukkit.getLogger().info(I18n.as("pulsegrasp.upload.disabled.log"));
        }

        if (url != null) {
            // the online report is the deliverable — nothing needs to stay on the server
            Bukkit.getLogger().info(I18n.as("pulsegrasp.upload.done.log", url));
            notifyUploadLink(uuid, url);
            return;
        }

        // upload failed or is switched off, so the report has to survive locally
        Path path = saveLocally(report);
        if (path == null) {
            // neither route worked — the report is gone, and only the console keeps the detail
            Bukkit.getLogger().warning(I18n.as("pulsegrasp.save.failed.log"));
            notifyPlayer(uuid, I18n.as("pulsegrasp.save.failed"));
            return;
        }

        Bukkit.getLogger().info(I18n.as("pulsegrasp.done.log", path));
        if (error != null) {
            Bukkit.getLogger().warning(I18n.as("pulsegrasp.upload.failed.log", error));
            notifyPlayer(uuid,
                    I18n.as("pulsegrasp.upload.failed", error),
                    I18n.as("pulsegrasp.done", path.toString()));
        } else {
            notifyPlayer(uuid, I18n.as("pulsegrasp.done", path.toString()));
        }
    }

    /** Write the report to the working directory; null when the write itself fails. */
    private static Path saveLocally(Report report) {
        Path path = Paths.get(report.fileName());
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write(report.json());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return path;
    }

    /** Human-readable size for the failure log. */
    private static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        }
        return String.format("%.2fMB", bytes / (1024.0 * 1024.0));
    }

    /** Send lines to the requesting player; no-op for console or offline players. */
    private void notifyPlayer(String uuid, String... messages) {
        withPlayer(uuid, player -> {
            for (String message : messages) {
                player.sendMessage(message);
            }
        });
    }

    /**
     * Send the share link as a clickable component. Minecraft does not linkify plain text,
     * so the URL needs an explicit OPEN_URL click event.
     */
    private void notifyUploadLink(String uuid, String url) {
        withPlayer(uuid, player -> ChatComponentAPI.sendClickOpenURLChat(
                player,
                I18n.as("pulsegrasp.upload.done", url),
                I18n.as("pulsegrasp.upload.hover"),
                url));
    }

    /** Run the action with the requesting player on the server thread; no-op when absent. */
    private void withPlayer(String uuid, Consumer<org.bukkit.entity.Player> action) {
        if (uuid == null || "none".equals(uuid)) return;
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (server == null) return;
        server.execute(() -> {
            try {
                org.bukkit.entity.Player player = Bukkit.getPlayer(java.util.UUID.fromString(uuid));
                if (player != null && player.isOnline()) {
                    action.accept(player);
                }
            } catch (IllegalArgumentException ignored) {
                // malformed UUID — nothing to notify
            }
        });
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

    /**
     * Record a tick with its measured duration (nanos) for precise MSPT stats.
     *
     * <p>Also picks up the configured tick rate: {@code /tick rate} can lower it at runtime, and a
     * low TPS is then by design. Without this the report would send someone looking for a fault
     * that is not there.
     */
    public static void markTick(long tickNanos) {
        if (!instance.grasping) {
            return;
        }
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (server == null) {
            instance.tickProfiler.markTick(tickNanos);
            return;
        }
        net.minecraft.server.ServerTickRateManager rates = server.tickRateManager();
        instance.tickProfiler.markTick(tickNanos, rates.tickrate(), rates.isFrozen(), rates.isSprinting());
    }

    public static void recordEntityPulse(String entityType, long nanos, java.util.UUID uuid, String world, int x, int y, int z) {
        if (instance.grasping) instance.tickProfiler.recordEntityPulse(entityType, nanos, uuid, world, x, y, z);
    }

    public static void recordBlockEntityPulse(String blockEntityType, long nanos, String world, int x, int y, int z) {
        if (instance.grasping) instance.tickProfiler.recordBlockEntityPulse(blockEntityType, nanos, world, x, y, z);
    }

    /**
     * How long the main thread spent waiting for async work before the next tick could start.
     * {@code startNanos} is 0 when not sampling; see recordChunkSourcePulse.
     */
    public static void recordAsyncWait(long startNanos) {
        if (startNanos != 0 && instance.grasping) {
            instance.tickProfiler.recordAsyncWait(System.nanoTime() - startNanos);
        }
    }

    public static void recordChunkStats(int totalChunks, int activeChunks) {
        if (instance.grasping) instance.tickProfiler.recordChunkStat(totalChunks, activeChunks);
    }

    /** markTick runs before the level ticks, so every level in a tick gets the same answer. */
    public static boolean shouldSampleChunkStats() {
        return instance.grasping
                && instance.tickProfiler.getTickCount() % CHUNK_STAT_INTERVAL_TICKS == 0;
    }

    /**
     * Record one sub-step of the chunk source tick. {@code startNanos} is the value captured before
     * the step ran, or 0 when not sampling — so the call site stays a single guarded line and a
     * missed sample is skipped rather than recorded as a bogus duration.
     *
     * <p>Call sites whose arguments are cheap (a string literal, a field read) may pass them
     * unconditionally. When building the argument costs anything at all — a registry lookup, a
     * {@code toString()} — wrap the call in {@code if (startNanos != 0)}, or the server pays for it
     * on every tick forever. See the block-event hook in {@code ServerLevel} for the pattern.
     */
    public static void recordChunkSourcePulse(String step, long startNanos) {
        if (startNanos != 0 && instance.grasping) {
            instance.tickProfiler.recordChunkSourcePulse(step, System.nanoTime() - startNanos);
        }
    }

    /** Record one processed block event against the block type that raised it; see above for startNanos. */
    public static void recordBlockEventPulse(String blockType, long startNanos) {
        if (startNanos != 0 && instance.grasping) {
            instance.tickProfiler.recordBlockEventPulse(blockType, System.nanoTime() - startNanos);
        }
    }

    /**
     * One movement sample for a player, taken once per tick. The player object is passed straight
     * through so nothing is extracted unless a run is actually in progress.
     */
    public static void recordPlayerMove(net.minecraft.server.level.ServerPlayer player) {
        if (!instance.grasping) {
            return;
        }
        instance.tickProfiler.recordPlayerMove(player.getUUID(), player.getName().getString(),
                player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ());
    }

    /** A chunk that just started loading because a player came near; see recordChunkSourcePulse. */
    public static void recordChunkTicket(long chunkPos) {
        if (instance.grasping) {
            instance.tickProfiler.recordChunkTicket(chunkPos);
        }
    }

    /** Per-chunk natural-spawn cost; {@code startNanos} is 0 when not sampling (see recordChunkSourcePulse). */
    public static void recordNaturalSpawn(String dimension, int chunkX, int chunkZ, long startNanos) {
        if (startNanos != 0 && instance.grasping) {
            instance.tickProfiler.recordNaturalSpawn(dimension, chunkX, chunkZ, System.nanoTime() - startNanos);
        }
    }

    /** Mob-cap/entity scan cost per dimension; {@code startNanos} is 0 when not sampling (see recordChunkSourcePulse). */
    public static void recordNaturalSpawnCount(String dimension, long startNanos) {
        if (startNanos != 0 && instance.grasping) {
            instance.tickProfiler.recordNaturalSpawnCount(dimension, System.nanoTime() - startNanos);
        }
    }

    public static int getTickCount() {
        return instance.tickProfiler.getTickCount();
    }

    public TickProfiler tickProfiler() {
        return tickProfiler;
    }

    public AsyncProfiler asyncProfiler() {
        return asyncProfiler;
    }

    public PacketProfiler packetProfiler() {
        return packetProfiler;
    }

    /**
     * Build the report in memory. It is only written to disk if the upload does not go through,
     * so the payload and the fallback file name are returned together.
     */
    private Report diagnose() {
        long durationMs = System.currentTimeMillis() - graspStartMs;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        JsonObject root = new JsonObject();
        root.addProperty("system", "PulseGrasp");
        root.addProperty("durationMs", durationMs);
        root.addProperty("durationSeconds", durationMs / 1000);

        JsonObject recordedBy = new JsonObject();
        JsonObject startBy = new JsonObject();
        startBy.addProperty("name", startName != null ? startName : "unknown");
        startBy.addProperty("uuid", startUuid != null ? startUuid : "none");
        recordedBy.add("start", startBy);
        // the run ends by itself, so the same identity closes it
        JsonObject stopBy = new JsonObject();
        stopBy.addProperty("name", startName != null ? startName : "unknown");
        stopBy.addProperty("uuid", startUuid != null ? startUuid : "none");
        recordedBy.add("stop", stopBy);
        root.add("recordedBy", recordedBy);

        JsonObject tickData = tickProfiler.toJson(durationMs);
        // Flatten every tick sub-field onto the root. This used to be a whitelist, which silently
        // dropped new fields (playerTrails / chunkLoads never reached the report). The one field
        // handled differently, blockEntityMeridians, is overwritten below with the merged version.
        for (Map.Entry<String, JsonElement> entry : tickData.entrySet()) {
            root.add(entry.getKey(), entry.getValue());
        }

        root.add("worldTickTimes", buildWorldTickTimes());

        JsonObject diagnosis = buildDiagnosis(durationMs,
                tickData.getAsJsonObject("tickStats"),
                tickData.getAsJsonArray("worstTicks"));
        root.add("diagnosis", diagnosis);

        // packet data — built once, used by both the block-entity merge and the network section
        JsonObject packetData = packetProfiler.toJson(durationMs);

        // block entities: tick time (blockEntityMeridians) + packet bytes (blockEntityStats)
        JsonObject beMerged = mergeBlockEntityData(
                tickData.getAsJsonArray("blockEntityMeridians"),
                packetData.getAsJsonObject("blockEntityStats")
        );
        root.add("blockEntityMeridians", beMerged);

        JsonObject networkSection = new JsonObject();
        networkSection.addProperty("totalBytes", packetData.get("totalBytes").getAsLong());
        networkSection.addProperty("totalPackets", packetData.get("totalPackets").getAsLong());
        networkSection.addProperty("bytesPerSecond", packetData.get("bytesPerSecond").getAsLong());
        networkSection.addProperty("packetsPerSecond", packetData.get("packetsPerSecond").getAsLong());
        networkSection.add("packetTypes", packetData.get("packetTypes"));
        networkSection.add("playerStats", packetData.get("playerStats"));
        networkSection.add("chunkStats", packetData.get("chunkStats"));
        networkSection.add("flowSeries", packetData.get("flowSeries"));
        // block-entity packets are already merged above
        root.add("network", networkSection);

        root.add("threadDump", threadProfiler.capture());
        root.add("methodSampler", methodSampler.toJson());
        root.add("asyncWorkers", asyncProfiler.toJson());
        root.add("systemSeries", systemProfiler.toJson());

        try {
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(root);
            return new Report(json, "pulse_grasp_" + timestamp + ".json");
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[PulseGrasp] report serialisation failed: " + t);
            return null;
        }
    }

    /** A built report: the JSON payload plus the file name to fall back to. */
    private record Report(String json, String fileName) {
    }

    /** Merge tick time and packet stats per block-entity type. */
    private JsonObject mergeBlockEntityData(JsonArray tickBeArray, JsonObject packetBeStats) {
        JsonObject merged = new JsonObject();

        java.util.Map<String, JsonObject> tickLookup = new java.util.LinkedHashMap<>();
        if (tickBeArray != null) {
            for (int i = 0; i < tickBeArray.size(); i++) {
                JsonObject entry = tickBeArray.get(i).getAsJsonObject();
                tickLookup.put(entry.get("name").getAsString(), entry);
            }
        }

        java.util.Map<String, JsonObject> packetLookup = new java.util.LinkedHashMap<>();
        if (packetBeStats != null && packetBeStats.has("types")) {
            JsonObject packetTypes = packetBeStats.getAsJsonObject("types");
            for (String key : packetTypes.keySet()) {
                packetLookup.put(key, packetTypes.getAsJsonObject(key));
            }
        }

        java.util.Set<String> allTypes = new java.util.LinkedHashSet<>();
        allTypes.addAll(tickLookup.keySet());
        allTypes.addAll(packetLookup.keySet());

        JsonObject mergedTypes = new JsonObject();

        for (String type : allTypes) {
            JsonObject entry = new JsonObject();

            JsonObject tickEntry = tickLookup.get(type);
            if (tickEntry != null) {
                copyProperty(tickEntry, entry, "totalMs");
                copyProperty(tickEntry, entry, "count");
                copyProperty(tickEntry, entry, "avgMs");
                copyProperty(tickEntry, entry, "percentage");
                copyProperty(tickEntry, entry, "topConsumers");
            }

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

        // tick entries first (by total time), then packet-only entries (by bytes)
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

        if (packetBeStats != null && packetBeStats.has("positions")) {
            merged.add("positions", packetBeStats.get("positions"));
        }

        return merged;
    }

    /** Per-dimension tick time from Paper's perWorldTickTimes. */
    private JsonArray buildWorldTickTimes() {
        JsonArray array = new JsonArray();
        try {
            net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
            if (server == null) return array;
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                long[] times = server.getTickTime(level.dimension());
                if (times == null) continue;
                long sum = 0;
                long max = 0;
                int n = 0;
                for (long t : times) {
                    if (t <= 0) continue;
                    sum += t;
                    if (t > max) max = t;
                    n++;
                }
                JsonObject obj = new JsonObject();
                obj.addProperty("dimension", level.dimension().location().toString());
                obj.addProperty("samples", n);
                obj.addProperty("avgMs", n > 0 ? String.format("%.2f", (double) sum / n / 1_000_000.0) : "0.00");
                obj.addProperty("maxMs", String.format("%.2f", (double) max / 1_000_000.0));
                array.add(obj);
            }
        } catch (Throwable ignored) {
            // never fail the report over an API mismatch
        }
        return array;
    }

    /**
     * Rank the most likely cause of low TPS / high MSPT.
     * Decision order: GC/memory > a dominant tick phase > overall load > transient spike > healthy.
     * The evidence behind the verdict (per-phase and per-entity time inside the slow ticks, GC
     * pause share, tick percentiles) is emitted alongside it.
     */
    private JsonObject buildDiagnosis(long durationMs, JsonObject tickStats, JsonArray worstTicks) {
        JsonObject d = new JsonObject();

        long samples = tickStats != null && tickStats.has("samples") ? tickStats.get("samples").getAsLong() : 0;
        double avg = num(tickStats, "measuredAvgMs");
        double max = num(tickStats, "measuredMaxMs");
        double p95 = num(tickStats, "p95Ms");
        double p99 = num(tickStats, "p99Ms");
        long slow = tickStats != null && tickStats.has("slowTicks") ? tickStats.get("slowTicks").getAsLong() : 0;

        JsonObject tickSummary = new JsonObject();
        tickSummary.addProperty("samples", samples);
        tickSummary.addProperty("avgMs", String.format("%.2f", avg));
        tickSummary.addProperty("p95Ms", String.format("%.2f", p95));
        tickSummary.addProperty("p99Ms", String.format("%.2f", p99));
        tickSummary.addProperty("maxMs", String.format("%.2f", max));
        tickSummary.addProperty("slowTicks", slow);
        tickSummary.addProperty("slowTickPercent", samples > 0 ? String.format("%.2f", slow * 100.0 / samples) : "0.00");
        d.add("tickSummary", tickSummary);

        long gcMs = systemProfiler.getTotalGcTimeMs();
        long gcCount = systemProfiler.getTotalGcCount();
        long gcMax = systemProfiler.getMaxGcPauseMs();
        double gcShare = durationMs > 0 ? gcMs * 100.0 / durationMs : 0;
        JsonObject gc = new JsonObject();
        gc.addProperty("totalPauseMs", gcMs);
        gc.addProperty("collections", gcCount);
        gc.addProperty("maxSingleSecondPauseMs", gcMax);
        gc.addProperty("pauseShareOfWallTimePercent", String.format("%.2f", gcShare));
        d.add("gc", gc);

        // aggregate the per-tick breakdown of all captured slow ticks
        Map<String, Long> phaseAgg = new LinkedHashMap<>();
        Map<String, Long> entityAgg = new LinkedHashMap<>();
        Map<String, Long> blockEntityAgg = new LinkedHashMap<>();
        long slowTickTotalNanos = 0;
        if (worstTicks != null) {
            for (JsonElement el : worstTicks) {
                JsonObject t = el.getAsJsonObject();
                slowTickTotalNanos += (long) (num(t, "tickMs") * 1_000_000.0);
                accumulate(t.getAsJsonArray("phases"), phaseAgg);
                accumulate(t.getAsJsonArray("entityTypes"), entityAgg);
                accumulate(t.getAsJsonArray("blockEntityTypes"), blockEntityAgg);
            }
        }
        d.add("slowTickPhases", topList(phaseAgg, slowTickTotalNanos, 10));
        d.add("slowTickEntityTypes", topList(entityAgg, slowTickTotalNanos, 10));
        d.add("slowTickBlockEntityTypes", topList(blockEntityAgg, slowTickTotalNanos, 10));

        // the composition of an average tick, as segments that add up to its real duration
        d.add("tickBudget", tickProfiler.sessionBudget());

        String domPhase = dominant(phaseAgg);
        long phaseTotal = sumValues(phaseAgg);
        double domShare = (domPhase != null && phaseTotal > 0) ? phaseAgg.get(domPhase) * 100.0 / phaseTotal : 0;
        double slowPct = samples > 0 ? slow * 100.0 / samples : 0;

        String verdict;
        StringBuilder summary = new StringBuilder();
        if (samples == 0) {
            verdict = "NO_DATA";
            summary.append("采样期间未捕获到 tick 数据。");
        } else if (gcShare >= 15 || (gcMax >= 200 && gcCount > 0)) {
            verdict = "GC_MEMORY";
            summary.append("主因倾向 GC/内存：GC 累计暂停 ").append(gcMs).append("ms（占墙钟 ")
                    .append(String.format("%.2f", gcShare)).append("%），单秒最大暂停 ").append(gcMax)
                    .append("ms。请检查堆大小、对象分配速率与 GC 日志。");
        } else if (slow > 0 && domPhase != null && domShare >= 35) {
            verdict = "TICK_PHASE";
            summary.append("主因倾向 tick 计算热点：慢 tick 中占比最高的阶段是「").append(domPhase)
                    .append("」（").append(String.format("%.1f", domShare)).append("%）");
            String topEntity = dominant(entityAgg);
            if (topEntity != null) summary.append("，最重的实体类型「").append(topEntity).append("」");
            String topBe = dominant(blockEntityAgg);
            if (topBe != null) summary.append("，最重的方块实体「").append(topBe).append("」");
            summary.append("。");
        } else if (slowPct >= 5) {
            verdict = "OVERALL_LOAD";
            summary.append("主因倾向整体负载过高：").append(String.format("%.1f", slowPct))
                    .append("% 的 tick 超过 50ms，且无单一阶段/实体主导。建议降低世界规模、视距或实体数量。");
        } else if (avg < 50 && slow > 0) {
            verdict = "TRANSIENT_SPIKE";
            summary.append("主因倾向偶发抖动：平均 MSPT ").append(String.format("%.2f", avg))
                    .append("ms 正常，但出现 ").append(slow).append(" 次超过 50ms 的慢 tick（P99=")
                    .append(String.format("%.2f", p99)).append("ms）。请查看「慢 tick 成因证据」里聚合出的阶段/实体。");
        } else if (avg < 50) {
            verdict = "HEALTHY";
            summary.append("未发现明显瓶颈：平均 MSPT ").append(String.format("%.2f", avg)).append("ms，无慢 tick。");
        } else {
            verdict = "TICK_PHASE";
            summary.append("平均 MSPT ").append(String.format("%.2f", avg)).append("ms 偏高");
            if (domPhase != null) summary.append("，占比最高阶段「").append(domPhase).append("」");
            summary.append("。");
        }
        d.addProperty("verdict", verdict);
        d.addProperty("summary", summary.toString());

        // natural-spawn evidence; the per-chunk breakdown is the naturalSpawnHeatmap section
        double spawnMs = tickProfiler.naturalSpawnTotalNanos() / 1_000_000.0;
        double spawnShare = durationMs > 0 ? spawnMs * 100.0 / durationMs : 0;
        JsonObject spawner = new JsonObject();
        spawner.addProperty("totalMs", String.format("%.2f", spawnMs));
        spawner.addProperty("shareOfWallTimePercent", String.format("%.2f", spawnShare));
        spawner.addProperty("perSecondMs", String.format("%.2f", durationMs > 0 ? spawnMs / (durationMs / 1000.0) : 0));
        d.add("naturalSpawn", spawner);
        if (spawnShare >= 10) {
            summary.append(" 其中自然生成（spawner）占墙钟 ").append(String.format("%.1f", spawnShare))
                    .append("%，逐区块热点见 naturalSpawnHeatmap。");
            d.addProperty("summary", summary.toString());
        }
        return d;
    }

    private static void accumulate(JsonArray arr, Map<String, Long> agg) {
        if (arr == null) return;
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            String name = s(o, "name");
            long nanos = (long) (num(o, "ms") * 1_000_000.0);
            agg.merge(name, nanos, Long::sum);
        }
    }

    private JsonArray topList(Map<String, Long> agg, long totalNanos, int limit) {
        JsonArray arr = new JsonArray();
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(agg.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < sorted.size() && i < limit; i++) {
            Map.Entry<String, Long> e = sorted.get(i);
            JsonObject o = new JsonObject();
            o.addProperty("name", e.getKey());
            o.addProperty("ms", String.format("%.3f", e.getValue() / 1_000_000.0));
            o.addProperty("percent", totalNanos > 0 ? String.format("%.2f", e.getValue() * 100.0 / totalNanos) : "0.00");
            arr.add(o);
        }
        return arr;
    }

    private static String dominant(Map<String, Long> agg) {
        String best = null;
        long bestVal = -1;
        for (Map.Entry<String, Long> e : agg.entrySet()) {
            if (e.getValue() > bestVal) {
                bestVal = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private static long sumValues(Map<String, Long> agg) {
        long total = 0;
        for (long v : agg.values()) total += v;
        return total;
    }

    private static String s(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return "0";
        return obj.get(key).getAsString();
    }

    private static double num(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return 0;
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return 0;
        }
    }

    private void copyProperty(JsonObject src, JsonObject dst, String key) {
        if (src.has(key)) {
            dst.add(key, src.get(key));
        }
    }
}