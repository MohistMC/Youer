package com.mohistmc.youer.feature.pulsegrasp;

import java.util.Map;

/**
 * 数据包统计 — 委托到 PulseGrasp 的 PacketProfiler 实例。
 * <p>
 * 作为向后兼容层，保留静态方法供 PacketEncoder 补丁调用。
 * 实际数据由 PulseGrasp 的 PacketProfiler 实例管理。
 */
public class PacketStatistics {

    public static void startStatisticsUpdater() {
        // 不再需要独立线程，由 PulseGrasp 管理
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
        // 不再独立保存，由 PulseGrasp 诊断报告统一输出
        return null;
    }
}