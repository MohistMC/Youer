package com.mohistmc.youer.feature.pulsegrasp;

import java.util.UUID;

/**
 * Common interface for instance trace data.
 */
interface TraceData {
    String world();
    int x();
    int y();
    int z();
    long totalNanos();
    int count();
}

/** Entity instance trace — keyed by UUID */
class EntityTrace implements TraceData {
    final UUID uuid;
    String world;
    int x, y, z;
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

    /** Refresh entity position — called on move to keep records accurate */
    void updatePosition(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override public String world() { return world; }
    @Override public int x() { return x; }
    @Override public int y() { return y; }
    @Override public int z() { return z; }
    @Override public long totalNanos() { return totalNanos; }
    @Override public int count() { return count; }
}

/** Block entity instance trace — keyed by position, with registered id (e.g. minecraft:furnace) */
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

/** A single vital-sign sample */
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

/** Chunk statistics sample (aggregated per dimension) — records total and active chunk counts */
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

/** A single system resource sample (JVM heap + process CPU usage) */
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