package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Samples every thread except the server one while a run is active, so a main-thread stall can be
 * attributed to something concrete instead of "it waited".
 *
 * <p>On a Minecraft server the interesting ones are the chunk system's background work
 * ({@code Worker-Main-N}, world generation) and its region-file IO ({@code IO-Worker-N}) — those are
 * what the main thread blocks on. Sampling is deliberately slow: {@code getThreadInfo} over every
 * thread is a safepoint operation, and a sampler that costs more than it measures is worse than
 * none.
 */
public class AsyncProfiler {

    /** One sample per second: fast enough to see the shape, cheap enough not to distort it. */
    private static final long SAMPLE_INTERVAL_MS = 1000L;
    private static final int MAX_DEPTH = 8;
    private static final int TOP_FRAMES = 8;
    private static final int MAX_GROUPS = 12;
    /** JVM housekeeping threads that would only add noise. */
    private static final Set<String> IGNORED = Set.of(
            "Reference Handler", "Finalizer", "Signal Dispatcher", "Common-Cleaner",
            "Attach Listener", "Notification Thread", "DestroyJavaVM", "process reaper",
            "Monitor Ctrl-Break", "SIGINT handler");
    /** The profiler's own samplers must not show up in the report they are producing. */
    private static final String OWN_THREAD_PREFIX = "PulseGrasp-";

    private final Map<String, Group> groups = new LinkedHashMap<>();
    private volatile boolean running;
    private Thread sampler;
    private long sampleCount;
    private String serverThreadName = "";

    void reset() {
        groups.clear();
        sampleCount = 0;
    }

    void start(String serverThreadName) {
        if (running) {
            return;
        }
        this.serverThreadName = serverThreadName == null ? "" : serverThreadName;
        running = true;
        sampler = new Thread(this::loop, "PulseGrasp-AsyncSampler");
        sampler.setDaemon(true);
        sampler.start();
    }

    void stop() {
        running = false;
        if (sampler != null) {
            sampler.interrupt();
            try {
                sampler.join(2000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sampler = null;
        }
    }

    private void loop() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                sample(bean);
                Thread.sleep(SAMPLE_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                // a sampler must never be the reason a run dies
                org.bukkit.Bukkit.getLogger().warning("[PulseGrasp] async sampler failed: " + t);
            }
        }
    }

    private void sample(ThreadMXBean bean) {
        ThreadInfo[] infos = bean.getThreadInfo(bean.getAllThreadIds(), MAX_DEPTH);
        if (infos == null) {
            return;
        }
        sampleCount++;
        for (ThreadInfo info : infos) {
            if (info == null) {
                continue;
            }
            String name = info.getThreadName();
            if (name == null || name.equals(serverThreadName) || IGNORED.contains(name)
                    || name.startsWith(OWN_THREAD_PREFIX)) {
                continue;
            }
            groups.computeIfAbsent(normalize(name), Group::new)
                    .observe(String.valueOf(info.getThreadState()), topFrame(info));
        }
    }

    /** {@code IO-Worker-3} and {@code IO-Worker-7} are the same pool, so the index is dropped. */
    private static String normalize(String name) {
        int dash = name.lastIndexOf('-');
        if (dash > 0 && dash < name.length() - 1
                && name.substring(dash + 1).chars().allMatch(Character::isDigit)) {
            return name.substring(0, dash);
        }
        return name;
    }

    /** The first frame that is not JDK plumbing — that is the one worth naming. */
    private static String topFrame(ThreadInfo info) {
        StackTraceElement[] stack = info.getStackTrace();
        if (stack == null || stack.length == 0) {
            return null;
        }
        for (StackTraceElement frame : stack) {
            String type = frame.getClassName();
            if (!type.startsWith("java.") && !type.startsWith("jdk.")
                    && !type.startsWith("sun.") && !type.startsWith("com.sun.")) {
                return type + "." + frame.getMethodName();
            }
        }
        return stack[0].getClassName() + "." + stack[0].getMethodName();
    }

    JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("samples", sampleCount);
        root.addProperty("intervalMs", SAMPLE_INTERVAL_MS);
        root.addProperty("maxDepth", MAX_DEPTH);
        JsonArray threads = new JsonArray();
        List<Group> sorted = groups.values().stream()
                .sorted((a, b) -> Integer.compare(b.samples, a.samples))
                .limit(MAX_GROUPS)
                .toList();
        for (Group group : sorted) {
            JsonObject o = new JsonObject();
            o.addProperty("thread", group.name);
            o.addProperty("samples", group.samples);
            JsonObject states = new JsonObject();
            group.states.forEach(states::addProperty);
            o.add("states", states);
            JsonArray frames = new JsonArray();
            group.frames.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(TOP_FRAMES)
                    .forEach(e -> {
                        JsonObject frame = new JsonObject();
                        frame.addProperty("frame", e.getKey());
                        frame.addProperty("samples", e.getValue());
                        frame.addProperty("percent", String.format("%.1f", e.getValue() * 100.0 / group.samples));
                        frames.add(frame);
                    });
            o.add("topFrames", frames);
            threads.add(o);
        }
        root.add("threads", threads);
        return root;
    }

    /** One thread group (pool), with what it was seen doing. */
    private static final class Group {
        final String name;
        int samples;
        final Map<String, Integer> states = new LinkedHashMap<>();
        final Map<String, Integer> frames = new LinkedHashMap<>();

        Group(String name) {
            this.name = name;
        }

        void observe(String state, String frame) {
            samples++;
            states.merge(state, 1, Integer::sum);
            if (frame != null) {
                frames.merge(frame, 1, Integer::sum);
            }
        }
    }
}
