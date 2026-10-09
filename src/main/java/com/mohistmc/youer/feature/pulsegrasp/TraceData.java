package com.mohistmc.youer.feature.pulsegrasp;

import java.util.UUID;

/** Instance trace data shared by entity and block entity traces. */
interface TraceData {
    String world();
    int x();
    int y();
    int z();
    long totalNanos();
    int count();
}

/** Entity trace, keyed by UUID. */
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

    /** Entities move, so refresh the recorded position. */
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

/** Block entity trace, keyed by position. */
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

/** One TPS/MSPT/ping sample. */
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

/** Chunk counts per dimension. */
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

/** One heap/CPU sample, with GC deltas for that second. */
class SystemSample {
    final long timestamp;
    final long heapUsedBytes;
    final long heapMaxBytes;
    final double cpuPercent;
    final long gcCount;
    final long gcTimeMs;

    SystemSample(long timestamp, long heapUsedBytes, long heapMaxBytes, double cpuPercent, long gcCount, long gcTimeMs) {
        this.timestamp = timestamp;
        this.heapUsedBytes = heapUsedBytes;
        this.heapMaxBytes = heapMaxBytes;
        this.cpuPercent = cpuPercent;
        this.gcCount = gcCount;
        this.gcTimeMs = gcTimeMs;
    }
}
