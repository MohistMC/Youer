package com.mohistmc.youer.feature.pulsegrasp;

import java.util.Map;

/**
 * Static facade kept for the PacketEncoder patch; all state lives in the
 * PacketProfiler owned by PulseGrasp.
 */
public class PacketStatistics {

    public static void startStatisticsUpdater() {
        // no thread needed any more
    }

    public static boolean isCollecting() {
        return PulseGrasp.isGrasping();
    }

    public static void updatePacketStats(String packetClassName, int bytes, String playerName) {
        if (!isCollecting()) return;
        if (PulseGrasp.isGrasping()) {
            PulseGrasp.instance().packetProfiler().updatePacketStats(packetClassName, bytes, playerName);
        }
    }

    public static void updateChunkPacketStats(String world, int x, int z, int bytes, String playerName) {
        if (!isCollecting()) return;
        if (PulseGrasp.isGrasping()) {
            PulseGrasp.instance().packetProfiler().updateChunkPacketStats(world, x, z, bytes, playerName);
        }
    }

    public static void updateBlockEntityStats(String world, int x, int y, int z, String type, int bytes) {
        if (!isCollecting()) return;
        if (PulseGrasp.isGrasping()) {
            PulseGrasp.instance().packetProfiler().updateBlockEntityStats(world, x, y, z, type, bytes);
        }
    }

    public static long getTotalBytesSent() {
        return PulseGrasp.isGrasping() ? PulseGrasp.instance().packetProfiler().getTotalBytes() : 0;
    }

    public static long getTotalPacketsSent() {
        return PulseGrasp.isGrasping() ? PulseGrasp.instance().packetProfiler().getTotalPackets() : 0;
    }

    public static Map<String, Long> getBytesByPacketType() {
        return PulseGrasp.isGrasping() ? PulseGrasp.instance().packetProfiler().getBytesByPacketType() : Map.of();
    }

    public static Map<String, Long> getPacketsByPacketType() {
        return PulseGrasp.isGrasping() ? PulseGrasp.instance().packetProfiler().getPacketsByPacketType() : Map.of();
    }

    public static long getBytesPerSecondByPacketType(String packetType) {
        return PulseGrasp.isGrasping() ? PulseGrasp.instance().packetProfiler().getBytesPerSecondByPacketType(packetType) : 0;
    }

    public static long getPacketsPerSecondByPacketType(String packetType) {
        return PulseGrasp.isGrasping() ? PulseGrasp.instance().packetProfiler().getPacketsPerSecondByPacketType(packetType) : 0;
    }

    public static long getBytesPerSecond() {
        return PulseGrasp.isGrasping() ? PulseGrasp.instance().packetProfiler().getBytesPerSecond() : 0;
    }

    public static long getPacketsPerSecond() {
        return PulseGrasp.isGrasping() ? PulseGrasp.instance().packetProfiler().getPacketsPerSecond() : 0;
    }

    public static Map<String, Map<String, Long>> getChunkStats() { return Map.of(); }
    public static Map<String, Map<String, Long>> getBlockEntityPosStats() { return Map.of(); }
    public static Map<String, Map<String, Long>> getBlockEntityTypeStats() { return Map.of(); }

    public static void resetStatistics() {
        if (PulseGrasp.isGrasping()) {
            PulseGrasp.instance().packetProfiler().reset();
        }
    }

    public static java.nio.file.Path savePacketStatsToJson() throws java.io.IOException {
        // folded into the PulseGrasp report, nothing to write here
        return null;
    }
}
