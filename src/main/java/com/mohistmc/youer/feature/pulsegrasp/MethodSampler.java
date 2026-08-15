package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Method-level sampler for PulseGrasp.
 *
 * <p>A background daemon thread periodically dumps the Server thread call stack and
 * aggregates per-method self / total sample counts. This yields a self-time breakdown
 * (which method spends the most CPU on the server thread) similar to spark, instead of
 * the coarser phase-level "meridians" produced by the injected tick pulses.
 *
 * <p>Precision &amp; clarity features:
 * <ul>
 *   <li>Method keys carry line numbers, so overloaded methods and exact hot spots are distinct.</li>
 *   <li>Frames are aggregated into a flame-graph call tree ({@link #root}), showing the call
 *       path that leads to each hot method, not just a flat list.</li>
 *   <li>Low-value infrastructure frames (JVM stdlib, common libs) are hidden so game / mod
 *       hot methods stand out.</li>
 *   <li>All sample states are tracked, so blocking (lock contention) and waiting (IO) time is
 *       reported separately from pure CPU consumption.</li>
 *   <li>Sampling interval / stack depth / top-N are configurable.</li>
 * </ul>
 *
 * <p>Only RUNNABLE samples contribute to the flame graph, so percentages reflect actual CPU
 * consumption rather than idle/waiting time. Sampling needs no source patches: it reads the
 * target thread's stack via {@link ThreadMXBean#getThreadInfo(long, int)}.
 */
public class MethodSampler {

    private static final ThreadMXBean TMB = ManagementFactory.getThreadMXBean();
    private static final int DEFAULT_MAX_DEPTH = 64;
    private static final long DEFAULT_INTERVAL_MS = 25;

    // Frames under these prefixes are low-value infrastructure and are hidden from the flame
    // graph so game / mod hot methods are not drowned out.
    private static final Set<String> FILTER_PREFIXES = Set.of(
            "java.", "javax.", "jdk.", "sun.", "com.sun.",
            "com.google.", "org.objectweb.", "it.unimi.dsi.", "org.slf4j.",
            "kotlin.", "org.jetbrains.", "org.apache.logging.");

    // className -> resolved namespace + kind. Resolving a class's origin is relatively
    // expensive (Class.forName + ProtectionDomain + ModList lookup), so cache every class.
    private final ConcurrentMap<String, NamespaceRef> classNamespaceCache = new ConcurrentHashMap<>();

    // ---- spark-style plugin classloader reflective handles ----
    // Bukkit/Paper load each plugin in its own PluginClassLoader which carries a reference
    // to the plugin instance. Reading that field yields the exact plugin.yml name, which is
    // more precise than inferring from the jar file name. All handles are resolved lazily and
    // cached; any of them may be null if the corresponding platform class is absent.
    private static final Class<?> PLUGIN_CLASS_LOADER = tryLoad("org.bukkit.plugin.java.PluginClassLoader");
    private static final Field PLUGIN_FIELD = PLUGIN_CLASS_LOADER == null ? null : tryField(PLUGIN_CLASS_LOADER, "plugin");
    private static final Class<?> PAPER_PLUGIN_CLASS_LOADER = tryLoad("io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader");
    private static final Field PAPER_PLUGIN_FIELD = PAPER_PLUGIN_CLASS_LOADER == null ? null : tryField(PAPER_PLUGIN_CLASS_LOADER, "loadedJavaPlugin");

    // Lazily collected classloaders of all loaded Bukkit plugins, so classes defined by
    // plugins (which live in child classloaders invisible to the server loader) can be found.
    private static volatile ClassLoader[] pluginLoaders;
    // The server tick thread's context classloader. On NeoForge this is the modlauncher
    // TransformingClassLoader that actually loads mod classes — unlike the sampler thread's
    // own context classloader, which cannot see them. Captured once at start().
    private static volatile ClassLoader serverContextLoader;

    // ---- configurable ----
    private volatile long intervalMs = DEFAULT_INTERVAL_MS;
    private volatile int maxDepth = DEFAULT_MAX_DEPTH;
    private volatile int maxTopN = 40;

    /** Per-thread-state sample counts, used to separate CPU vs blocked/waiting time. */
    private final Map<String, Integer> stateCounts = new LinkedHashMap<>();
    /** Flat per-method aggregation (denormalised from the call tree for quick scanning). */
    private final Map<String, MethodAgg> flatStats = new HashMap<>();
    /** Flame-graph call tree. Root's children are leaf (deepest) frames. */
    private final FrameNode root = new FrameNode("root");

    private Thread samplerThread;
    private volatile boolean running;
    private long targetThreadId = -1;
    private long startCpuNanos = -1;
    private long totalCpuNanos = 0;
    private volatile int totalSamples = 0;
    private volatile int runnableSamples = 0;

    // ---- config API ----

    public MethodSampler setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
        return this;
    }

    public MethodSampler setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public MethodSampler setMaxTopN(int maxTopN) {
        this.maxTopN = maxTopN;
        return this;
    }

    // ---- lifecycle ----

    void start(long serverThreadId) {
        this.targetThreadId = serverThreadId;
        this.running = true;
        this.flatStats.clear();
        this.root.children.clear();
        this.root.totalCount = 0;
        this.root.selfCount = 0;
        this.stateCounts.clear();
        this.totalSamples = 0;
        this.runnableSamples = 0;
        this.totalCpuNanos = 0;
        this.startCpuNanos = TMB.getThreadCpuTime(serverThreadId);
        // Capture the server thread's context classloader (NeoForge TransformingClassLoader)
        // so mod classes can be found during namespace resolution.
        Thread serverThread = findThread(serverThreadId);
        serverContextLoader = serverThread != null ? serverThread.getContextClassLoader() : null;
        samplerThread = new Thread(this::loop, "PulseGrasp-MethodSampler");
        samplerThread.setDaemon(true);
        samplerThread.start();
    }

    private static Thread findThread(long id) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    void stop() {
        running = false;
        if (samplerThread != null) {
            samplerThread.interrupt();
            try {
                samplerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            samplerThread = null;
        }
        if (targetThreadId != -1) {
            long end = TMB.getThreadCpuTime(targetThreadId);
            if (startCpuNanos > 0 && end > 0) {
                totalCpuNanos = Math.max(0, end - startCpuNanos);
            }
        }
    }

    private void loop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            long t0 = System.nanoTime();
            sampleOnce();
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
            long sleep = intervalMs - elapsedMs;
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // ---- sampling ----

    private void sampleOnce() {
        ThreadInfo info = TMB.getThreadInfo(targetThreadId, maxDepth);
        if (info == null) return;
        totalSamples++;
        Thread.State state = info.getThreadState();
        stateCounts.merge(state.name(), 1, Integer::sum);
        // Only RUNNABLE samples count as CPU consumption and build the flame graph.
        if (state != Thread.State.RUNNABLE) return;
        runnableSamples++;

        StackTraceElement[] raw = info.getStackTrace();
        if (raw == null || raw.length == 0) return;

        // Drop low-value frames so game / mod hot paths stay connected and visible.
        List<StackTraceElement> stack = new ArrayList<>(raw.length);
        for (StackTraceElement e : raw) {
            if (!isFiltered(e)) stack.add(e);
        }
        if (stack.isEmpty()) return;

        // Flat aggregation on the filtered frames.
        for (int i = 0; i < stack.size(); i++) {
            String key = keyOf(stack.get(i));
            MethodAgg agg = flatStats.computeIfAbsent(key, k -> new MethodAgg());
            agg.totalCount++;
            if (i == 0) agg.selfCount++;
            // Store only the call path from this method upward (its own frame + callers), so
            // sibling methods in the same chain don't all share the identical full stack.
            if (agg.sampleStack == null) {
                agg.sampleStack = stack.subList(i, stack.size()).toArray(new StackTraceElement[0]);
            }
        }

        // Flame-graph trie: walk from the leaf (index 0) up to the entry point.
        FrameNode node = root;
        node.totalCount++;
        for (int i = 0; i < stack.size(); i++) {
            String key = keyOf(stack.get(i));
            node = node.children.computeIfAbsent(key, k -> new FrameNode(key));
            node.totalCount++;
            if (i == 0) node.selfCount++;
            if (node.sampleStack == null) {
                node.sampleStack = stack.subList(i, stack.size()).toArray(new StackTraceElement[0]);
            }
        }
    }

    private static boolean isFiltered(StackTraceElement e) {
        String cls = e.getClassName();
        for (String prefix : FILTER_PREFIXES) {
            if (cls.startsWith(prefix)) return true;
        }
        return false;
    }

    /** Method key includes the line number so overloads and exact hot spots are distinct. */
    private static String keyOf(StackTraceElement e) {
        String loc = e.isNativeMethod() ? "(native)" : ":" + e.getLineNumber();
        return e.getClassName() + "." + e.getMethodName() + loc;
    }

    /** Format a fully-qualified key as {@code SimpleClass.method:line}. */
    static String displayName(String key) {
        int lastDot = key.lastIndexOf('.');
        if (lastDot <= 0) return key;
        String cls = key.substring(0, lastDot);
        String methodPart = key.substring(lastDot);
        int pkgDot = cls.lastIndexOf('.');
        String simple = pkgDot >= 0 ? cls.substring(pkgDot + 1) : cls;
        return simple + methodPart;
    }

    // ---- JSON output ----

    JsonObject toJson() {
        JsonObject rootJson = new JsonObject();
        rootJson.addProperty("intervalMs", intervalMs);
        rootJson.addProperty("maxDepth", maxDepth);
        rootJson.addProperty("totalSamples", totalSamples);
        rootJson.addProperty("runnableSamples", runnableSamples);
        rootJson.addProperty("totalCpuMs", totalCpuNanos / 1_000_000);
        rootJson.addProperty("filteredFramesHidden", FILTER_PREFIXES.size());

        // Diagnostic: how many distinct sampled classes resolved to each namespace kind.
        // Helps tell apart "no plugin code was sampled" from "plugin code was sampled but
        // misclassified". Logged so the operator can see it without opening the JSON.
        int[] kindCounts = new int[NamespaceKind.values().length];
        for (String key : flatStats.keySet()) {
            kindCounts[resolveNamespace(classNameOf(key)).kind.ordinal()]++;
        }
        org.bukkit.Bukkit.getLogger().info("[PulseGrasp] sampled " + flatStats.size()
                + " distinct classes -> plugin:" + kindCounts[NamespaceKind.PLUGIN.ordinal()]
                + " mod:" + kindCounts[NamespaceKind.MOD.ordinal()]
                + " unknown:" + kindCounts[NamespaceKind.UNKNOWN.ordinal()]
                + " (runnableSamples=" + runnableSamples + ")");

        // Thread-state distribution: separates pure CPU from blocking and waiting.
        JsonObject state = new JsonObject();
        for (Map.Entry<String, Integer> e : stateCounts.entrySet()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("count", e.getValue());
            entry.addProperty("percent", pct(e.getValue(), totalSamples));
            state.add(e.getKey(), entry);
        }
        rootJson.add("state", state);

        // Mod/plugin hotspot breakdown — kept as separate sections, not merged together.
        rootJson.add("pluginHotspots", buildHotspots(NamespaceKind.PLUGIN));
        rootJson.add("modHotspots", buildHotspots(NamespaceKind.MOD));

        // Flame-graph call tree.
        rootJson.add("flameTree", toJson(root, 0));

        // Flat hot-method list (by self time) for quick scanning.
        List<Map.Entry<String, MethodAgg>> sorted = flatStats.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue().selfCount))
                .limit(maxTopN)
                .toList();
        JsonArray methods = new JsonArray();
        for (Map.Entry<String, MethodAgg> e : sorted) {
            MethodAgg agg = e.getValue();
            JsonObject obj = new JsonObject();
            obj.addProperty("name", displayName(e.getKey()));
            obj.addProperty("signature", e.getKey());
            obj.addProperty("selfCount", agg.selfCount);
            obj.addProperty("totalCount", agg.totalCount);
            obj.addProperty("selfPercent", pct(agg.selfCount, runnableSamples));
            obj.addProperty("totalPercent", pct(agg.totalCount, runnableSamples));
            obj.addProperty("selfCpuMs", selfCpuMs(agg.selfCount, runnableSamples));
            if (agg.sampleStack != null) {
                obj.addProperty("sampleStack", formatStack(agg.sampleStack));
            }
            methods.add(obj);
        }
        rootJson.add("topMethods", methods);

        return rootJson;
    }

    private JsonObject toJson(FrameNode node, int depth) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", depth == 0 ? "root" : displayName(node.key));
        obj.addProperty("selfCount", node.selfCount);
        obj.addProperty("totalCount", node.totalCount);
        obj.addProperty("selfPercent", pct(node.selfCount, runnableSamples));
        obj.addProperty("totalPercent", pct(node.totalCount, runnableSamples));
        obj.addProperty("selfCpuMs", selfCpuMs(node.selfCount, runnableSamples));
        if (node.sampleStack != null) {
            obj.addProperty("sampleStack", formatStack(node.sampleStack));
        }

        if (!node.children.isEmpty()) {
            // Keep the tree readable by expanding only the top hottest children.
            List<Map.Entry<String, FrameNode>> children = node.children.entrySet().stream()
                    .sorted(Comparator.comparingInt(e -> -e.getValue().selfCount))
                    .limit(maxTopN)
                    .toList();
            JsonArray childArray = new JsonArray();
            for (Map.Entry<String, FrameNode> child : children) {
                childArray.add(toJson(child.getValue(), depth + 1));
            }
            obj.add("children", childArray);
        }
        return obj;
    }

    private String selfCpuMs(int selfCount, int runnableSamples) {
        if (runnableSamples <= 0 || selfCount <= 0) return "0.00";
        return String.format("%.2f", (double) totalCpuNanos / 1_000_000 * (double) selfCount / runnableSamples);
    }

    private static String pct(int count, int total) {
        if (total <= 0) return "0.00";
        return String.format("%.2f", (double) count / total * 100);
    }

    private static String formatStack(StackTraceElement[] stack) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e : stack) {
            sb.append("  at ").append(e).append('\n');
        }
        return sb.toString();
    }

    // ---- mod/plugin hotspot breakdown ----

    /**
     * Build the per-namespace hotspot breakdown for one kind (PLUGIN or MOD). Mods and
     * plugins are kept as separate top-level report sections instead of being merged.
     */
    private JsonArray buildHotspots(NamespaceKind kind) {
        Map<String, ModAgg> groups = new LinkedHashMap<>();
        for (Map.Entry<String, MethodAgg> e : flatStats.entrySet()) {
            String key = e.getKey();
            MethodAgg agg = e.getValue();
            String cls = classNameOf(key);
            NamespaceRef ref = resolveNamespace(cls);
            if (ref.kind != kind) continue;
            ModAgg ma = groups.computeIfAbsent(ref.namespace, k -> new ModAgg());
            ma.selfCount += agg.selfCount;
            ma.totalCount += agg.totalCount;
            // Track the hot methods inside each namespace so the report shows WHY it is hot.
            MethodAgg method = ma.methods.computeIfAbsent(key, k -> new MethodAgg());
            method.selfCount += agg.selfCount;
            method.totalCount += agg.totalCount;
            if (method.sampleStack == null) method.sampleStack = agg.sampleStack;
        }

        List<Map.Entry<String, ModAgg>> sorted = groups.entrySet().stream()
                .filter(e2 -> e2.getValue().selfCount > 0)
                .sorted(Comparator.comparingInt(e2 -> -e2.getValue().selfCount))
                .toList();

        JsonArray array = new JsonArray();
        for (Map.Entry<String, ModAgg> e2 : sorted) {
            ModAgg ma = e2.getValue();
            JsonObject obj = new JsonObject();
            obj.addProperty("namespace", e2.getKey());
            obj.addProperty("selfCount", ma.selfCount);
            obj.addProperty("selfPercent", pct(ma.selfCount, runnableSamples));
            obj.addProperty("totalCount", ma.totalCount);
            obj.addProperty("totalPercent", pct(ma.totalCount, runnableSamples));
            obj.addProperty("selfCpuMs", selfCpuMs(ma.selfCount, runnableSamples));

            // Hot methods inside this namespace, each with its own captured stack trace.
            List<Map.Entry<String, MethodAgg>> top = ma.methods.entrySet().stream()
                    .sorted(Comparator.comparingInt(x -> -x.getValue().selfCount))
                    .limit(maxTopN)
                    .toList();
            JsonArray topMethods = new JsonArray();
            for (Map.Entry<String, MethodAgg> m : top) {
                MethodAgg agg = m.getValue();
                JsonObject mo = new JsonObject();
                mo.addProperty("name", displayName(m.getKey()));
                mo.addProperty("signature", m.getKey());
                mo.addProperty("selfCount", agg.selfCount);
                mo.addProperty("totalCount", agg.totalCount);
                mo.addProperty("selfPercent", pct(agg.selfCount, runnableSamples));
                if (agg.sampleStack != null) {
                    mo.addProperty("sampleStack", formatStack(agg.sampleStack));
                }
                topMethods.add(mo);
            }
            obj.add("topMethods", topMethods);
            array.add(obj);
        }
        return array;
    }

    /** Extract the fully-qualified class name from a method key ("Class.method:line"). */
    private static String classNameOf(String key) {
        int methodDot = key.lastIndexOf('.');
        if (methodDot <= 0) return key;
        return key.substring(0, methodDot);
    }

    /** Map a fully-qualified class name to a categorized namespace (plugin / mod / unknown), with caching. */
    private NamespaceRef resolveNamespace(String className) {
        NamespaceRef cached = classNamespaceCache.get(className);
        if (cached != null) return cached;
        NamespaceRef ref = resolveFromClass(className);
        if (ref == null) ref = new NamespaceRef("unknown", NamespaceKind.UNKNOWN);
        classNamespaceCache.put(className, ref);
        return ref;
    }

    /**
     * Resolve the mod/plugin behind a method. Spark-style: load the class, then
     * <ul>
     *   <li>if it was defined by a Bukkit/Paper PluginClassLoader, read the plugin instance
     *       and return its exact plugin.yml name (kind PLUGIN);</li>
     *   <li>otherwise attribute it to a NeoForge mod id / jar (kind MOD);</li>
     * </ul>
     * Returns null if the class cannot be located or has no attributable source.
     */
    private static NamespaceRef resolveFromClass(String className) {
        for (ClassLoader loader : allCandidateLoaders()) {
            try {
                Class<?> cls = Class.forName(className, false, loader);
                String jar = jarNameOfCodeSource(cls);
                // 1. Read the exact plugin.yml name from the Bukkit/Paper plugin classloader.
                String plugin = pluginNameOf(cls);
                if (plugin != null) return new NamespaceRef(plugin, NamespaceKind.PLUGIN);
                // 2. On hybrid (Mohist) servers a plugin class may sit in a non-standard
                //    classloader that pluginNameOf cannot introspect. Fall back to mapping
                //    the plugin jar base name to its plugin.yml name.
                if (jar != null) {
                    String pluginFromJar = pluginNameOfJar(jar);
                    if (pluginFromJar != null) return new NamespaceRef(pluginFromJar, NamespaceKind.PLUGIN);
                }
                // 3. Otherwise attribute the class to a NeoForge mod id, or its jar.
                String modId = modIdOf(cls);
                if (modId != null) return new NamespaceRef(modId, NamespaceKind.MOD);
                if (jar != null) {
                    // getModContainerByClass only matches the @Mod main class, so map the
                    // jar to a mod id for the majority of mod classes (spark-style).
                    String modIdFromJar = modIdOfJar(jar);
                    return new NamespaceRef(modIdFromJar != null ? modIdFromJar : jar, NamespaceKind.MOD);
                }
            } catch (ClassNotFoundException | LinkageError e) {
                // not visible to this loader — try the next one
            } catch (Throwable t) {
                // protection-domain / class-loading edge cases: skip this loader and keep
                // trying the remaining ones, otherwise a single failing loader would hide
                // all plugin classes (they only resolve via the later plugin loaders).
                continue;
            }
        }
        return null;
    }

    /** Exact plugin.yml name if the class was defined by a Bukkit/Paper plugin classloader. */
    private static String pluginNameOf(Class<?> cls) {
        ClassLoader loader = cls.getClassLoader();
        if (loader == null) return null;
        Field field = null;
        if (PLUGIN_CLASS_LOADER != null && PLUGIN_CLASS_LOADER.isInstance(loader)) {
            field = PLUGIN_FIELD;
        } else if (PAPER_PLUGIN_CLASS_LOADER != null && PAPER_PLUGIN_CLASS_LOADER.isInstance(loader)) {
            field = PAPER_PLUGIN_FIELD;
        }
        if (field == null) return null;
        try {
            Object plugin = field.get(loader);
            if (plugin == null) return null;
            Method getName = plugin.getClass().getMethod("getName");
            return (String) getName.invoke(plugin);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Jar base name the class was loaded from, or null if it has no class source. */
    private static String jarNameOfCodeSource(Class<?> cls) {
        CodeSource cs = cls.getProtectionDomain().getCodeSource();
        if (cs == null || cs.getLocation() == null) return null;
        String jarName = jarNameOf(cs.getLocation());
        return jarName == null ? null : cleanJarName(jarName);
    }

    /**
     * Resolve the NeoForge mod id behind a class. Mirrors spark: {@code ModList.get()
     * .getModContainerByClass(cls).map(ModContainer::getModId)}. Done purely via reflection
     * so MethodSampler never hard-depends on the FML loader classes.
     */
    private static String modIdOf(Class<?> cls) {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList == null) return null;
            Object result = modListClass.getMethod("getModContainerByClass", Class.class).invoke(modList, cls);
            if (result == null) return null;
            // result is java.util.Optional<? extends ModContainer> — unwrap; empty means not a mod class.
            Object container = result.getClass().getMethod("orElse", Object.class).invoke(result, (Object) null);
            if (container == null) return null;
            return (String) container.getClass().getMethod("getModId").invoke(container);
        } catch (Throwable t) {
            return null;
        }
    }

    // jarName (e.g. "goblintraders-neoforge-1.21.1-1.11.2") -> modId (e.g. "goblintraders")
    // Built lazily from ModList.getMods() so EVERY mod class maps to a clean mod id.
    private static volatile Map<String, String> jarNameToModId;

    private static String modIdOfJar(String jarName) {
        if (jarNameToModId == null) {
            synchronized (MethodSampler.class) {
                if (jarNameToModId == null) {
                    jarNameToModId = buildJarNameToModId();
                }
            }
        }
        String id = jarNameToModId.get(jarName);
        if (id != null) return id;
        // Try the cleaned name too — some mods have versioned jar names but the @Mod
        // file reference may be the un-versioned counterpart.
        String cleaned = cleanJarName(jarName);
        if (!cleaned.equals(jarName)) {
            id = jarNameToModId.get(cleaned);
        }
        return id;
    }

    /**
     * Iterate over all mod containers, mapping each mod's jar file base name to its modId.
     * Uses reflection so there is no hard dependency on FML classes.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> buildJarNameToModId() {
        Map<String, String> map = new HashMap<>();
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            // ModList.get() -> ModList instance
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList == null) return map;
            // ModList.getMods() -> List<IModInfo>
            Object mods = modListClass.getMethod("getMods").invoke(modList);
            if (!(mods instanceof List)) return map;
            for (Object info : (List<Object>) mods) {
                String modId = (String) info.getClass().getMethod("getModId").invoke(info);
                // IModInfo.getOwningFile() -> IModFileInfo (can be null)
                Object fileInfo;
                try {
                    fileInfo = info.getClass().getMethod("getOwningFile").invoke(info);
                } catch (NoSuchMethodException e) {
                    // fallback for older NeoForge: getModFileInfo()
                    fileInfo = info.getClass().getMethod("getModFileInfo").invoke(info);
                }
                if (fileInfo == null) continue;
                // IModFileInfo.getFile() -> IModFile
                Object modFile = fileInfo.getClass().getMethod("getFile").invoke(fileInfo);
                if (modFile == null) continue;
                // IModFile.getFilePath() -> Path
                Object filePath = modFile.getClass().getMethod("getFilePath").invoke(modFile);
                if (filePath == null) continue;
                // Path.getFileName() -> Path (just the filename part)
                Object fileName = filePath.getClass().getMethod("getFileName").invoke(filePath);
                if (fileName == null) continue;
                String name = fileName.toString();
                // Strip .jar/.zip extension
                if (name.endsWith(".jar")) name = name.substring(0, name.length() - 4);
                else if (name.endsWith(".zip")) name = name.substring(0, name.length() - 4);
                if (!name.isEmpty() && modId != null) {
                    map.put(name, modId);
                    // Also index by the version-stripped name so it matches the cleaned name
                    // produced by jarNameOfCodeSource() (e.g. "goblintraders-neoforge").
                    String cleaned = cleanJarName(name);
                    if (!cleaned.equals(name)) map.put(cleaned, modId);
                }
            }
        } catch (Throwable ignored) {
            // If reflection fails, the map will be empty and we fall back to jar names.
        }
        return map;
    }

    // jar base name (e.g. "essentialsx-2.20.0") -> plugin.yml name (e.g. "Essentials").
    // Built lazily by walking every loaded Bukkit plugin, so plugin classes resolve to the
    // exact plugin name even when their classloader cannot be introspected (Mohist hybrid).
    private static volatile Map<String, String> jarNameToPluginName;

    private static String pluginNameOfJar(String jarName) {
        if (jarNameToPluginName == null) {
            synchronized (MethodSampler.class) {
                if (jarNameToPluginName == null) {
                    jarNameToPluginName = buildJarNameToPluginName();
                }
            }
        }
        String name = jarNameToPluginName.get(jarName);
        if (name != null) return name;
        String cleaned = cleanJarName(jarName);
        if (!cleaned.equals(jarName)) {
            name = jarNameToPluginName.get(cleaned);
        }
        return name;
    }

    /**
     * Map each loaded plugin's jar base name to its plugin.yml name. The jar is located via
     * the plugin's own classloader rather than the loader class, so it points at the plugin
     * jar regardless of the loader implementation.
     */
    private static Map<String, String> buildJarNameToPluginName() {
        Map<String, String> map = new HashMap<>();
        try {
            org.bukkit.plugin.Plugin[] plugins = org.bukkit.Bukkit.getPluginManager().getPlugins();
            for (org.bukkit.plugin.Plugin plugin : plugins) {
                String pluginName = plugin.getName();
                String jarName = pluginJarName(plugin);
                if (jarName == null) continue;
                map.put(jarName, pluginName);
                String cleaned = cleanJarName(jarName);
                if (!cleaned.equals(jarName)) map.put(cleaned, pluginName);
            }
        } catch (Throwable ignored) {
            // If Bukkit is unavailable, the map stays empty and plugins fall back to MOD naming.
        }
        return map;
    }

    /** Determine the jar base name a plugin was loaded from. */
    private static String pluginJarName(org.bukkit.plugin.Plugin plugin) {
        // Prefer the plugin.yml resource URL inside the plugin's own classloader; it reliably
        // points into the plugin jar regardless of the loader class used by the server.
        try {
            ClassLoader loader = plugin.getClass().getClassLoader();
            if (loader != null) {
                URL yml = loader.getResource("plugin.yml");
                if (yml != null) {
                    String name = jarNameOf(yml);
                    if (name != null) return name;
                }
            }
        } catch (Throwable ignored) {
        }
        // Fall back to JavaPlugin.getFile() when available.
        try {
            if (plugin instanceof org.bukkit.plugin.java.JavaPlugin) {
                File file = ((org.bukkit.plugin.java.JavaPlugin) plugin).getFile();
                if (file != null) {
                    String name = file.getName();
                    if (name.endsWith(".jar")) name = name.substring(0, name.length() - 4);
                    return name;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Server loaders plus every loaded plugin's classloader (plugin classes are invisible to the server loader). */
    private static ClassLoader[] allCandidateLoaders() {
        ClassLoader[] base = candidateClassLoaders();
        ClassLoader[] plugins = pluginLoaders();
        // Prepend the server thread context loader — the NeoForge TransformingClassLoader
        // that actually defines mod classes. Must come first so mod classes resolve before
        // the (failing) app-loader attempts.
        int extra = serverContextLoader != null ? 1 : 0;
        ClassLoader[] all = new ClassLoader[base.length + plugins.length + extra];
        int idx = 0;
        if (extra == 1) all[idx++] = serverContextLoader;
        System.arraycopy(base, 0, all, idx, base.length);
        idx += base.length;
        System.arraycopy(plugins, 0, all, idx, plugins.length);
        return all;
    }

    private static ClassLoader[] candidateClassLoaders() {
        return new ClassLoader[] {
                MethodSampler.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader(),
                ClassLoader.getSystemClassLoader()
        };
    }

    /** Collect the classloaders of all loaded Bukkit plugins, cached across calls. */
    private static ClassLoader[] pluginLoaders() {
        ClassLoader[] cached = pluginLoaders;
        if (cached != null) return cached;
        try {
            org.bukkit.plugin.Plugin[] plugins = org.bukkit.Bukkit.getPluginManager().getPlugins();
            ClassLoader[] loaders = new ClassLoader[plugins.length];
            for (int i = 0; i < plugins.length; i++) {
                loaders[i] = plugins[i].getClass().getClassLoader();
            }
            pluginLoaders = loaders;
            return loaders;
        } catch (Throwable t) {
            pluginLoaders = new ClassLoader[0];
            return pluginLoaders;
        }
    }

    /** Extract the jar base name (without ".jar") from a class source location. */
    private static String jarNameOf(URL location) {
        try {
            String path = location.toURI().getPath();
            if (path == null) return null;
            // Drop any URL fragment ("#...") that some loaders append to the location.
            int hash = path.indexOf('#');
            if (hash >= 0) path = path.substring(0, hash);
            // jar:file:/.../mod.jar!/com/x  -> keep only the jar path before "!/".
            int exclamation = path.indexOf("!/");
            if (exclamation >= 0) path = path.substring(0, exclamation);
            // Also strip a trailing "!/" if present.
            if (path.endsWith("!/")) path = path.substring(0, path.length() - 2);
            int slash = path.lastIndexOf('/');
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            if (name.endsWith(".jar")) {
                return name.substring(0, name.length() - 4);
            }
            // unpacked / directory source — use the directory name instead
            return name.isEmpty() ? null : name;
        } catch (Exception e) {
            return null;
        }
    }

    private static Class<?> tryLoad(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field tryField(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Strip version suffixes from a jar base name, e.g. "create-1.21.1-0.5.1" -> "create". */
    private static String cleanJarName(String jarName) {
        String cleaned;
        while (!(cleaned = jarName.replaceFirst("-\\d+(\\.\\w+)*$", "")).equals(jarName)) {
            jarName = cleaned;
        }
        return jarName;
    }

    private static class ModAgg {
        int selfCount;
        int totalCount;
        final Map<String, MethodAgg> methods = new LinkedHashMap<>();
    }

    /** Category of a resolved namespace, so mods and plugins are reported separately. */
    enum NamespaceKind {
        PLUGIN, MOD, UNKNOWN
    }

    /** A resolved class origin: a namespace plus the kind it belongs to. */
    static final class NamespaceRef {
        final String namespace;
        final NamespaceKind kind;

        NamespaceRef(String namespace, NamespaceKind kind) {
            this.namespace = namespace;
            this.kind = kind;
        }
    }

    private static class MethodAgg {
        int selfCount;
        int totalCount;
        StackTraceElement[] sampleStack;
    }

    private static class FrameNode {
        final String key;
        int selfCount;
        int totalCount;
        StackTraceElement[] sampleStack;
        final Map<String, FrameNode> children = new LinkedHashMap<>();

        FrameNode(String key) {
            this.key = key;
        }
    }
}