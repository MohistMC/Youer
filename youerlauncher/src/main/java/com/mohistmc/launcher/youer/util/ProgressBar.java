package com.mohistmc.launcher.youer.util;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicLong;

public class ProgressBar implements AutoCloseable {

    private final String task;
    private final long total;
    private final int barWidth;
    private final PrintStream out;

    private final AtomicLong current = new AtomicLong(0);
    private final long startTimeNanos;
    private volatile boolean closed = false;
    private int lastLineLength = 0;

    private final char filledChar;
    private final char emptyChar;

    public ProgressBar(String task, long total) {
        this(task, total, 30, System.out);
    }

    public ProgressBar(String task, long total, int barWidth, PrintStream out) {
        this.task = task;
        this.total = Math.max(total, 1);
        this.barWidth = barWidth;
        this.out = out;
        this.startTimeNanos = System.nanoTime();

        this.filledChar = '=';
        this.emptyChar = ' ';

        render();
    }

    public void step() {
        step(1);
    }

    public void step(long n) {
        if (closed) return;
        current.addAndGet(n);
        render();
    }

    public void setTo(long value) {
        if (closed) return;
        current.set(value);
        render();
    }

    public long getCurrent() {
        return current.get();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        current.set(total);
        render();
        out.println();
        out.flush();
    }

    private synchronized void render() {
        long cur = Math.min(current.get(), total);
        int percent = (int) (cur * 100 / total);
        int filled = (int) (cur * barWidth / total);

        StringBuilder sb = new StringBuilder();
        sb.append('\r');
        sb.append(task).append(' ');
        sb.append('[');
        for (int i = 0; i < barWidth; i++) {
            sb.append(i < filled ? filledChar : emptyChar);
        }
        sb.append(']');
        sb.append(String.format(" %3d%% (%d/%d)", percent, cur, total));

        long elapsedNanos = System.nanoTime() - startTimeNanos;
        if (elapsedNanos > 500_000_000L && cur > 0) {
            double elapsedSec = elapsedNanos / 1_000_000_000.0;
            double speed = cur / elapsedSec;
            sb.append(String.format(" %.1f/s", speed));
            if (cur < total) {
                long etaSec = (long) ((total - cur) / speed);
                sb.append(String.format(" ETA:%02d:%02d", etaSec / 60, etaSec % 60));
            }
        }

        // 空格覆盖残留（替代 ANSI \033[2K）
        int pad = Math.max(0, lastLineLength - sb.length());
        for (int i = 0; i < pad; i++) {
            sb.append(' ');
        }

        out.print(sb);
        out.flush();
        lastLineLength = sb.length();
    }
}