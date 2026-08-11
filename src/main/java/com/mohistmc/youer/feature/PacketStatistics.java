package com.mohistmc.youer.feature;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.Getter;

public class PacketStatistics {
    // Traffic statistics
    private static final AtomicLong totalBytesSent = new AtomicLong(0);
    private static final AtomicLong totalPacketsSent = new AtomicLong(0);

    // Per-second traffic statistics (sliding window)
    private static final AtomicLong currentSecondBytes = new AtomicLong(0);
    private static final AtomicLong currentSecondPackets = new AtomicLong(0);
    // Traffic statistics by packet type
    private static final Map<String, AtomicLong> bytesByPacketType = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> packetsByPacketType = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> currentSecondBytesByPacketType = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> currentSecondPacketsByPacketType = new ConcurrentHashMap<>();
    private static final Map<String, Long> bytesPerSecondByPacketType = new ConcurrentHashMap<>();
    private static final Map<String, Long> packetsPerSecondByPacketType = new ConcurrentHashMap<>();
    // Per-chunk statistics for ClientboundLevelChunkWithLightPacket
    private static final Map<String, AtomicLong> chunkBytes = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> chunkPackets = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> chunkPlayers = new ConcurrentHashMap<>();
    // Per-player statistics
    private static final Map<String, AtomicLong> playerBytes = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> playerPackets = new ConcurrentHashMap<>();
    // Per-packet-type player list
    private static final Map<String, Set<String>> packetTypePlayers = new ConcurrentHashMap<>();
    // Per-block-entity statistics for ClientboundBlockEntityDataPacket
    private static final Map<String, AtomicLong> bePosBytes = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> bePosPackets = new ConcurrentHashMap<>();
    private static final Map<String, String> bePosTypes = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> beTypeBytes = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> beTypePackets = new ConcurrentHashMap<>();
    private static volatile long lastSecond = System.currentTimeMillis() / 1000;
    @Getter
    private static volatile long bytesPerSecond = 0;
    @Getter
    private static volatile long packetsPerSecond = 0;
    // Statistics update thread
    private static Thread updaterThread;
    private static volatile boolean running = false;
    // Check if data is being collected
    @Getter
    private static volatile boolean collecting = false; // Whether data is being collected
    // Add method to get start time
    @Getter
    private static volatile long startTime = 0;

    public static void startStatisticsUpdater() {
        if (running) return;

        running = true;
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
        }, "PacketEncoder-Stats-Updater");
        updaterThread.setDaemon(true);
        updaterThread.start();
    }

    public static void stopStatisticsUpdater() {
        running = false;
        collecting = false;
        if (updaterThread != null) {
            updaterThread.interrupt();
        }
        resetStatistics();
    }

    public static void startCollecting() {
        collecting = true;
        startTime = System.currentTimeMillis(); // Record start time
        resetStatistics(); // Reset statistics when starting a new collection session
    }

    // Stop data collection
    public static void stopCollecting() {
        collecting = false;
    }

    private static void updateSecondlyStats() {
        long currentSecond = System.currentTimeMillis() / 1000;
        if (currentSecond > lastSecond) {
            bytesPerSecond = currentSecondBytes.getAndSet(0);
            packetsPerSecond = currentSecondPackets.getAndSet(0);
            lastSecond = currentSecond;

            // Update per-second statistics by packet type
            bytesPerSecondByPacketType.clear();
            packetsPerSecondByPacketType.clear();

            for (Map.Entry<String, AtomicLong> entry : currentSecondBytesByPacketType.entrySet()) {
                String packetType = entry.getKey();
                long bytes = entry.getValue().getAndSet(0);
                bytesPerSecondByPacketType.put(packetType, bytes);
            }

            for (Map.Entry<String, AtomicLong> entry : currentSecondPacketsByPacketType.entrySet()) {
                String packetType = entry.getKey();
                long packets = entry.getValue().getAndSet(0);
                packetsPerSecondByPacketType.put(packetType, packets);
            }
        }
    }

    // Update packet statistics
    public static void updatePacketStats(String packetClassName, int bytes, @org.jetbrains.annotations.Nullable String playerName) {
        if (!collecting) return; // If collection is not enabled, do not record data

        // Update global statistics
        totalBytesSent.addAndGet(bytes);
        totalPacketsSent.incrementAndGet();
        currentSecondBytes.addAndGet(bytes);
        currentSecondPackets.incrementAndGet();

        // Update statistics by packet type
        bytesByPacketType.computeIfAbsent(packetClassName, k -> new AtomicLong(0)).addAndGet(bytes);
        packetsByPacketType.computeIfAbsent(packetClassName, k -> new AtomicLong(0)).incrementAndGet();
        currentSecondBytesByPacketType.computeIfAbsent(packetClassName, k -> new AtomicLong(0)).addAndGet(bytes);
        currentSecondPacketsByPacketType.computeIfAbsent(packetClassName, k -> new AtomicLong(0)).incrementAndGet();

        // Update per-player statistics
        if (playerName != null) {
            playerBytes.computeIfAbsent(playerName, k -> new AtomicLong(0)).addAndGet(bytes);
            playerPackets.computeIfAbsent(playerName, k -> new AtomicLong(0)).incrementAndGet();
            packetTypePlayers.computeIfAbsent(packetClassName, k -> ConcurrentHashMap.newKeySet()).add(playerName);
        }
    }

    // Update per-chunk statistics for ClientboundLevelChunkWithLightPacket
    public static void updateChunkPacketStats(String world, int x, int z, int bytes, @org.jetbrains.annotations.Nullable String playerName) {
        if (!collecting) return;
        String key = world + ":" + x + "," + z;
        chunkBytes.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(bytes);
        chunkPackets.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        if (playerName != null) {
            chunkPlayers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(playerName);
        }
    }

    // Update per-block-entity statistics for ClientboundBlockEntityDataPacket
    public static void updateBlockEntityStats(String world, int x, int y, int z, String type, int bytes) {
        if (!collecting) return;
        String posKey = world + ":" + x + "," + y + "," + z;
        bePosBytes.computeIfAbsent(posKey, k -> new AtomicLong(0)).addAndGet(bytes);
        bePosPackets.computeIfAbsent(posKey, k -> new AtomicLong(0)).incrementAndGet();
        bePosTypes.putIfAbsent(posKey, type);
        beTypeBytes.computeIfAbsent(type, k -> new AtomicLong(0)).addAndGet(bytes);
        beTypePackets.computeIfAbsent(type, k -> new AtomicLong(0)).incrementAndGet();
    }

    // Provide access methods for statistics
    public static long getTotalBytesSent() {
        return totalBytesSent.get();
    }

    public static long getTotalPacketsSent() {
        return totalPacketsSent.get();
    }

    // Access methods for statistics by packet type
    public static Map<String, Long> getBytesByPacketType() {
        return bytesByPacketType.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    public static Map<String, Long> getPacketsByPacketType() {
        return packetsByPacketType.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    public static long getBytesPerSecondByPacketType(String packetType) {
        return bytesPerSecondByPacketType.getOrDefault(packetType, 0L);
    }

    public static long getPacketsPerSecondByPacketType(String packetType) {
        return packetsPerSecondByPacketType.getOrDefault(packetType, 0L);
    }

    public static Map<String, Map<String, Long>> getChunkStats() {
        return chunkBytes.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            Map<String, Long> stats = new java.util.HashMap<>();
                            stats.put("bytes", e.getValue().get());
                            stats.put("packets", chunkPackets.getOrDefault(e.getKey(), new AtomicLong(0)).get());
                            return stats;
                        }));
    }

    public static Map<String, Map<String, Long>> getBlockEntityPosStats() {
        return bePosBytes.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            Map<String, Long> stats = new java.util.HashMap<>();
                            stats.put("bytes", e.getValue().get());
                            stats.put("packets", bePosPackets.getOrDefault(e.getKey(), new AtomicLong(0)).get());
                            return stats;
                        }));
    }

    public static Map<String, Map<String, Long>> getBlockEntityTypeStats() {
        return beTypeBytes.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            Map<String, Long> stats = new java.util.HashMap<>();
                            stats.put("bytes", e.getValue().get());
                            stats.put("packets", beTypePackets.getOrDefault(e.getKey(), new AtomicLong(0)).get());
                            return stats;
                        }));
    }

    // Reset statistics
    public static void resetStatistics() {
        totalBytesSent.set(0);
        totalPacketsSent.set(0);
        currentSecondBytes.set(0);
        currentSecondPackets.set(0);
        bytesPerSecond = 0;
        packetsPerSecond = 0;

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

    public static java.nio.file.Path savePacketStatsToJson() throws java.io.IOException {
        java.nio.file.Path saveDir = java.nio.file.Paths.get("packetstats");
        if (!java.nio.file.Files.exists(saveDir)) {
            java.nio.file.Files.createDirectories(saveDir);
        }

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = java.time.LocalDateTime.now().format(formatter);
        java.nio.file.Path filePath = saveDir.resolve("packetstats_" + timestamp + ".json");

        com.google.gson.JsonObject root = new com.google.gson.JsonObject();

        root.addProperty("totalBytes", PacketStatistics.getTotalBytesSent());
        root.addProperty("totalPackets", PacketStatistics.getTotalPacketsSent());
        root.addProperty("bytesPerSecond", PacketStatistics.getBytesPerSecond());
        root.addProperty("packetsPerSecond", PacketStatistics.getPacketsPerSecond());
        root.addProperty("timestamp", timestamp);
        root.addProperty("durationMs", System.currentTimeMillis() - PacketStatistics.getStartTime());
        root.addProperty("durationSeconds", (System.currentTimeMillis() - PacketStatistics.getStartTime()) / 1000);

        com.google.gson.JsonObject packetTypes = new com.google.gson.JsonObject();
        Map<String, Long> bytesByPacketType = PacketStatistics.getBytesByPacketType();

        bytesByPacketType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String packetType = entry.getKey();
                    long bytes = entry.getValue();
                    long packets = PacketStatistics.getPacketsByPacketType().getOrDefault(packetType, 0L);
                    long bytesPerSecond = PacketStatistics.getBytesPerSecondByPacketType(packetType);
                    long packetsPerSecond = PacketStatistics.getPacketsPerSecondByPacketType(packetType);

                    com.google.gson.JsonObject packetStats = new com.google.gson.JsonObject();
                    packetStats.addProperty("bytes", bytes);
                    packetStats.addProperty("packets", packets);
                    packetStats.addProperty("avgBytesPerPacket", packets > 0 ? bytes / packets : 0);
                    packetStats.addProperty("bandwidthPercentage", (double) bytes / PacketStatistics.getTotalBytesSent() * 100);
                    packetStats.addProperty("bytesPerSecond", bytesPerSecond);
                    packetStats.addProperty("packetsPerSecond", packetsPerSecond);

                    com.google.gson.JsonArray players = new com.google.gson.JsonArray();
                    Set<String> playerSet = packetTypePlayers.get(packetType);
                    if (playerSet != null) {
                        playerSet.forEach(players::add);
                    }
                    packetStats.add("players", players);

                    packetTypes.add(packetType, packetStats);
                });

        root.add("packetTypes", packetTypes);

        com.google.gson.JsonObject playerStats = new com.google.gson.JsonObject();
        playerBytes.forEach((name, bytes) -> {
            com.google.gson.JsonObject entry = new com.google.gson.JsonObject();
            entry.addProperty("bytes", bytes.get());
            entry.addProperty("packets", playerPackets.getOrDefault(name, new AtomicLong(0)).get());
            playerStats.add(name, entry);
        });
        root.add("playerStats", playerStats);

        com.google.gson.JsonObject chunkStats = new com.google.gson.JsonObject();
        chunkBytes.forEach((pos, bytes) -> {
            com.google.gson.JsonObject chunkEntry = new com.google.gson.JsonObject();
            chunkEntry.addProperty("bytes", bytes.get());
            chunkEntry.addProperty("packets", chunkPackets.getOrDefault(pos, new AtomicLong(0)).get());
            com.google.gson.JsonArray players = new com.google.gson.JsonArray();
            Set<String> playerSet = chunkPlayers.get(pos);
            if (playerSet != null) {
                playerSet.forEach(players::add);
            }
            chunkEntry.add("players", players);
            chunkStats.add(pos, chunkEntry);
        });
        root.add("chunkStats", chunkStats);

        com.google.gson.JsonObject blockEntityStats = new com.google.gson.JsonObject();
        com.google.gson.JsonObject bePositions = new com.google.gson.JsonObject();
        bePosBytes.forEach((pos, bytes) -> {
            com.google.gson.JsonObject entry = new com.google.gson.JsonObject();
            entry.addProperty("bytes", bytes.get());
            entry.addProperty("packets", bePosPackets.getOrDefault(pos, new AtomicLong(0)).get());
            entry.addProperty("type", bePosTypes.getOrDefault(pos, "unknown"));
            bePositions.add(pos, entry);
        });
        blockEntityStats.add("positions", bePositions);

        com.google.gson.JsonObject beTypes = new com.google.gson.JsonObject();
        PacketStatistics.getBlockEntityTypeStats().forEach((type, stats) -> {
            com.google.gson.JsonObject entry = new com.google.gson.JsonObject();
            entry.addProperty("bytes", stats.get("bytes"));
            entry.addProperty("packets", stats.get("packets"));
            beTypes.add(type, entry);
        });
        blockEntityStats.add("types", beTypes);
        root.add("blockEntityStats", blockEntityStats);

        try (java.io.FileWriter writer = new java.io.FileWriter(filePath.toFile())) {
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
        }

        return filePath;
    }
}