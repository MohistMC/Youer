package com.mohistmc.youer.feature.pulsegrasp;

import java.util.UUID;

/**
 * 实例追踪数据通用接口
 */
interface TraceData {
    String world();
    int x();
    int y();
    int z();
    long totalNanos();
    int count();
}

/** 实体实例追踪 — 按 UUID 标识 */
class EntityTrace implements TraceData {
    final UUID uuid;
    final String world;
    final int x, y, z;
    long totalNanos;
    int count;

    EntityTrace(UUID uuid, String world, int x, int y, int z) {
        this.uuid = uuid;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    void accumulate(long nanos) {
        totalNanos += nanos;
        count++;
    }

    @Override public String world() { return world; }
    @Override public int x() { return x; }
    @Override public int y() { return y; }
    @Override public int z() { return z; }
    @Override public long totalNanos() { return totalNanos; }
    @Override public int count() { return count; }
}

/** 方块实体实例追踪 — 按坐标标识，附带注册 id（如 minecraft:furnace） */
class BlockEntityTrace implements TraceData {
    final String type;
    final String world;
    final int x, y, z;
    long totalNanos;
    int count;

    BlockEntityTrace(String type, String world, int x, int y, int z) {
        this.type = type;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    void accumulate(long nanos) {
        totalNanos += nanos;
        count++;
    }

    @Override public String world() { return world; }
    @Override public int x() { return x; }
    @Override public int y() { return y; }
    @Override public int z() { return z; }
    @Override public long totalNanos() { return totalNanos; }
    @Override public int count() { return count; }
}

/** 一次生命体征采样 */
class VitalSign {
    final long timestamp;
    final double tps;
    final double mspt;
    final int ping;

    VitalSign(long timestamp, double tps, double mspt, int ping) {
        this.timestamp = timestamp;
        this.tps = tps;
        this.mspt = mspt;
        this.ping = ping;
    }
}

/** 区块统计采样（按维度聚合）— 记录总区块数与活跃区块数 */
class ChunkStat {
    int samples;
    long totalSum;
    long activeSum;
    int latestTotal;
    int latestActive;
    int maxTotal;
    int maxActive;

    void accumulate(int total, int active) {
        samples++;
        totalSum += total;
        activeSum += active;
        latestTotal = total;
        latestActive = active;
        maxTotal = Math.max(maxTotal, total);
        maxActive = Math.max(maxActive, active);
    }
}

/** 一次系统资源采样（JVM 堆内存 + 进程 CPU 使用率） */
class SystemSample {
    final long timestamp;
    final long heapUsedBytes;
    final long heapMaxBytes;
    final double cpuPercent;

    SystemSample(long timestamp, long heapUsedBytes, long heapMaxBytes, double cpuPercent) {
        this.timestamp = timestamp;
        this.heapUsedBytes = heapUsedBytes;
        this.heapMaxBytes = heapMaxBytes;
        this.cpuPercent = cpuPercent;
    }
}