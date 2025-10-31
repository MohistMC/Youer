package com.mohistmc.youer.feature;

import java.util.Map;
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
    public static void updatePacketStats(String packetClassName, int bytes) {
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

    // Reset statistics
    public static void resetStatistics() {
        totalBytesSent.set(0);
        totalPacketsSent.set(0);
        currentSecondBytes.set(0);
        currentSecondPackets.set(0);
        bytesPerSecond = 0;
        packetsPerSecond = 0;

        bytesByPacketType.values().forEach(counter -> counter.set(0));
        packetsByPacketType.values().forEach(counter -> counter.set(0));
        currentSecondBytesByPacketType.values().forEach(counter -> counter.set(0));
        currentSecondPacketsByPacketType.values().forEach(counter -> counter.set(0));
        bytesPerSecondByPacketType.clear();
        packetsPerSecondByPacketType.clear();
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
                    packetStats.addProperty("bytesPerSecond", bytesPerSecond);
                    packetStats.addProperty("packetsPerSecond", packetsPerSecond);

                    packetTypes.add(packetType, packetStats);
                });

        root.add("packetTypes", packetTypes);

        try (java.io.FileWriter writer = new java.io.FileWriter(filePath.toFile())) {
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
        }

        return filePath;
    }
}