package io.papermc.paper.util;

public class KeepAlive {

    public long lastKeepAliveTx = System.nanoTime();
    public static final record KeepAliveResponse(long txTimeNS, long rxTimeNS) {
        public long latencyNS() {
            return this.rxTimeNS - this.txTimeNS;
        }
    }
    public static final record PendingKeepAlive(long txTimeNS, long challengeId) {}

    public final ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue<PendingKeepAlive> pendingKeepAlives = new ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue<>();

    public final PingCalculator pingCalculator1m = new PingCalculator(java.util.concurrent.TimeUnit.MINUTES.toNanos(1L));
    public final PingCalculator pingCalculator5s = new PingCalculator(java.util.concurrent.TimeUnit.SECONDS.toNanos(5L));

    public KeepAlive copyForListenerHandoff() {
        KeepAlive copy = new KeepAlive();
        copy.lastKeepAliveTx = this.lastKeepAliveTx;
        copy.pingCalculator1m.copyFrom(this.pingCalculator1m);
        copy.pingCalculator5s.copyFrom(this.pingCalculator5s);
        return copy;
    }

    public static final class PingCalculator {

        private final long intervalNS;
        private final ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue<KeepAliveResponse> responses = new ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue<>();

        private long timeSumNS;
        private int timeSumCount;
        private volatile long lastAverageNS;

        public PingCalculator(long intervalNS) {
            this.intervalNS = intervalNS;
        }

        private void copyFrom(PingCalculator other) {
            for (KeepAliveResponse response : other.responses) {
                this.responses.add(response);
            }
            this.timeSumNS = other.timeSumNS;
            this.timeSumCount = other.timeSumCount;
            this.lastAverageNS = other.lastAverageNS;
        }

        public void update(KeepAliveResponse response) {
            long currTime = response.txTimeNS;

            this.responses.add(response);

            ++this.timeSumCount;
            this.timeSumNS += response.latencyNS();

            // remove out-of-window times
            KeepAliveResponse removed;
            while ((removed = this.responses.pollIf((ka) -> (currTime - ka.txTimeNS) > this.intervalNS)) != null) {
                --this.timeSumCount;
                this.timeSumNS -= removed.latencyNS();
            }

            this.lastAverageNS = this.timeSumNS / (long)this.timeSumCount;
        }

        public int getAvgLatencyMS() {
            return (int)java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(this.getAvgLatencyNS());
        }

        public long getAvgLatencyNS() {
            return this.lastAverageNS;
        }

        public it.unimi.dsi.fastutil.longs.LongArrayList getAllNS() {
            it.unimi.dsi.fastutil.longs.LongArrayList ret = new it.unimi.dsi.fastutil.longs.LongArrayList();

            for (KeepAliveResponse response : this.responses) {
                ret.add(response.latencyNS());
            }

            return ret;
        }
    }
}
