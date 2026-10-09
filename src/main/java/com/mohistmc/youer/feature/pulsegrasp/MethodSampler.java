package com.mohistmc.youer.feature.pulsegrasp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
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
 * Method-level sampler for PulseGrasp: a background thread periodically dumps the server
 * thread's stack and aggregates per-method self / total sample counts, giving a self-time
 * breakdown similar to spark rather than the coarser phase-level "meridians".
 * <p>
 * Only RUNNABLE samples build the flame graph, so percentages reflect CPU consumption rather
 * than idle or waiting time. Sampling reads the target thread's stack via
 * {@link ThreadMXBean#getThreadInfo(long, int)} — no source patches needed.
 */
public class MethodSampler {

    private static final ThreadMXBean TMB = ManagementFactory.getThreadMXBean();
    private static final int DEFAULT_MAX_DEPTH = 64;
    private static final long DEFAULT_INTERVAL_MS = 25;

    // Hidden from the flame graph so game / mod hot methods are not drowned out.
    private static final Set<String> FILTER_PREFIXES = Set.of(
            "java.", "javax.", "jdk.", "sun.", "com.sun.",
            "com.google.", "org.objectweb.", "it.unimi.dsi.", "org.slf4j.",
            "kotlin.", "org.jetbrains.", "org.apache.logging.");

    // Resolving a class's origin costs a Class.forName + ProtectionDomain + ModList lookup,
    // so every class is cached.
    private final ConcurrentMap<String, NamespaceRef> classNamespaceCache = new ConcurrentHashMap<>();

    // Bukkit/Paper load each plugin in its own PluginClassLoader holding a reference to the
    // plugin instance; reading that field yields the exact plugin.yml name, which beats
    // inferring from the jar file name. Handles resolve lazily and may be null.
    private static final Class<?> PLUGIN_CLASS_LOADER = tryLoad("org.bukkit.plugin.java.PluginClassLoader");
    private static final Field PLUGIN_FIELD = PLUGIN_CLASS_LOADER == null ? null : tryField(PLUGIN_CLASS_LOADER, "plugin");
    private static final Class<?> PAPER_PLUGIN_CLASS_LOADER = tryLoad("io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader");
    private static final Field PAPER_PLUGIN_FIELD = PAPER_PLUGIN_CLASS_LOADER == null ? null : tryField(PAPER_PLUGIN_CLASS_LOADER, "loadedJavaPlugin");

    // Plugin classes live in child classloaders that are invisible to the server loader.
    private static volatile ClassLoader[] pluginLoaders;
    // The server tick thread's context classloader. On NeoForge this is the modlauncher
    // TransformingClassLoader that actually defines mod classes — unlike the sampler thread's
    // own context loader. Captured once at start().
    private static volatile ClassLoader serverContextLoader;

    // Sampling is no longer configurable at runtime; these are fixed defaults.
    private final long intervalMs = DEFAULT_INTERVAL_MS;
    private final int maxDepth = DEFAULT_MAX_DEPTH;
    private final int maxTopN = 40;

    /** Per-thread-state sample counts, to separate CPU from blocked / waiting time. */
    private final Map<String, Integer> stateCounts = new LinkedHashMap<>();
    /** Flat per-method aggregation, for quick scanning. */
    private final Map<String, MethodAgg> flatStats = new HashMap<>();
    /** Flame-graph call tree; root's children are the leaf (deepest) frames. */
    private final FrameNode root = new FrameNode("root");

    private Thread samplerThread;
    private volatile boolean running;
    private long targetThreadId = -1;
    private long startCpuNanos = -1;
    private long totalCpuNanos = 0;
    private volatile int totalSamples = 0;
    private volatile int runnableSamples = 0;

    void start(long serverThreadId) {
        stop(); // never leave a previous sampler writing into the same maps
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
        // the server thread's context loader is what can actually see mod classes
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

    private void sampleOnce() {
        ThreadInfo info = TMB.getThreadInfo(targetThreadId, maxDepth);
        if (info == null) return;
        totalSamples++;
        Thread.State state = info.getThreadState();
        stateCounts.merge(state.name(), 1, Integer::sum);
        // only RUNNABLE counts as CPU consumption
        if (state != Thread.State.RUNNABLE) return;
        runnableSamples++;

        StackTraceElement[] raw = info.getStackTrace();
        if (raw == null || raw.length == 0) return;

        List<StackTraceElement> stack = new ArrayList<>(raw.length);
        for (StackTraceElement e : raw) {
            if (!isFiltered(e)) stack.add(e);
        }
        if (stack.isEmpty()) return;

        for (int i = 0; i < stack.size(); i++) {
            String key = keyOf(stack.get(i));
            MethodAgg agg = flatStats.computeIfAbsent(key, k -> new MethodAgg());
            agg.totalCount++;
            if (i == 0) agg.selfCount++;
            // keep only the path from this frame upward, so siblings don't share one full stack
            if (agg.sampleStack == null) {
                agg.sampleStack = stack.subList(i, stack.size()).toArray(new StackTraceElement[0]);
            }
        }

        // flame-graph trie: walk from the leaf (index 0) up to the entry point
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

    /** Key includes the line number, so overloads stay distinct. */
    private static String keyOf(StackTraceElement e) {
        String loc = e.isNativeMethod() ? "(native)" : ":" + e.getLineNumber();
        return e.getClassName() + "." + e.getMethodName() + loc;
    }

    /** "Class.method:line" -> "SimpleClass.method:line". */
    static String displayName(String key) {
        int lastDot = key.lastIndexOf('.');
        if (lastDot <= 0) return key;
        String cls = key.substring(0, lastDot);
        String methodPart = key.substring(lastDot);
        int pkgDot = cls.lastIndexOf('.');
        String simple = pkgDot >= 0 ? cls.substring(pkgDot + 1) : cls;
        return simple + methodPart;
    }

    JsonObject toJson() {
        JsonObject rootJson = new JsonObject();
        rootJson.addProperty("intervalMs", intervalMs);
        rootJson.addProperty("maxDepth", maxDepth);
        rootJson.addProperty("totalSamples", totalSamples);
        rootJson.addProperty("runnableSamples", runnableSamples);
        rootJson.addProperty("totalCpuMs", totalCpuNanos / 1_000_000);
        rootJson.addProperty("filteredFramesHidden", FILTER_PREFIXES.size());

        // how many sampled classes resolved to each kind — tells "no plugin code sampled"
        // apart from "plugin code sampled but misclassified"
        int[] kindCounts = new int[NamespaceKind.values().length];
        for (String key : flatStats.keySet()) {
            kindCounts[resolveNamespace(classNameOf(key)).kind.ordinal()]++;
        }
        org.bukkit.Bukkit.getLogger().info("[PulseGrasp] sampled " + flatStats.size()
                + " distinct classes -> plugin:" + kindCounts[NamespaceKind.PLUGIN.ordinal()]
                + " mod:" + kindCounts[NamespaceKind.MOD.ordinal()]
                + " unknown:" + kindCounts[NamespaceKind.UNKNOWN.ordinal()]
                + " (runnableSamples=" + runnableSamples + ")");

        // separates pure CPU from blocking and waiting
        JsonObject state = new JsonObject();
        for (Map.Entry<String, Integer> e : stateCounts.entrySet()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("count", e.getValue());
            entry.addProperty("percent", pct(e.getValue(), totalSamples));
            state.add(e.getKey(), entry);
        }
        rootJson.add("state", state);

        // mods and plugins stay as separate sections, never merged
        rootJson.add("pluginHotspots", buildHotspots(NamespaceKind.PLUGIN));
        rootJson.add("modHotspots", buildHotspots(NamespaceKind.MOD));

        rootJson.add("flameTree", toJson(root, 0));

        // flat hot-method list by self time
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
            // expand only the hottest children, to keep the tree readable
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

    /** Per-namespace hotspot breakdown for one kind (PLUGIN or MOD). */
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
            // keep the hot methods of each namespace, so the report shows why it is hot
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

    /** "Class.method:line" -> "Class". */
    private static String classNameOf(String key) {
        int methodDot = key.lastIndexOf('.');
        if (methodDot <= 0) return key;
        return key.substring(0, methodDot);
    }

    /** Class name -> namespace, with caching. */
    private NamespaceRef resolveNamespace(String className) {
        NamespaceRef cached = classNamespaceCache.get(className);
        if (cached != null) return cached;
        NamespaceRef ref = resolveFromClass(className);
        if (ref == null) ref = new NamespaceRef("unknown", NamespaceKind.UNKNOWN);
        classNamespaceCache.put(className, ref);
        return ref;
    }

    /**
     * Resolve the mod / plugin behind a class: a Bukkit or Paper plugin classloader yields the
     * exact plugin.yml name (PLUGIN), otherwise the class is attributed to a NeoForge mod id or
     * its jar (MOD). Returns null when the class cannot be located.
     */
    private static NamespaceRef resolveFromClass(String className) {
        for (ClassLoader loader : allCandidateLoaders()) {
            try {
                Class<?> cls = Class.forName(className, false, loader);
                String jar = jarNameOfCodeSource(cls);
                String plugin = pluginNameOf(cls);
                if (plugin != null) return new NamespaceRef(plugin, NamespaceKind.PLUGIN);
                // On hybrid (Mohist) servers a plugin class may sit in a classloader that
                // pluginNameOf cannot introspect, so fall back to the plugin jar's base name.
                if (jar != null) {
                    String pluginFromJar = pluginNameOfJar(jar);
                    if (pluginFromJar != null) return new NamespaceRef(pluginFromJar, NamespaceKind.PLUGIN);
                }
                String modId = modIdOf(cls);
                if (modId != null) return new NamespaceRef(modId, NamespaceKind.MOD);
                if (jar != null) {
                    // getModContainerByClass only matches the @Mod main class, so map the jar
                    // to a mod id for the majority of mod classes (spark-style).
                    String modIdFromJar = modIdOfJar(jar);
                    return new NamespaceRef(modIdFromJar != null ? modIdFromJar : jar, NamespaceKind.MOD);
                }
            } catch (ClassNotFoundException | LinkageError e) {
                // not visible to this loader — try the next one
            } catch (Throwable t) {
                // a single failing loader must not hide all plugin classes, which only
                // resolve through the later plugin loaders
                continue;
            }
        }
        return null;
    }

    /** Exact plugin.yml name when the class was defined by a Bukkit/Paper plugin classloader. */
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

    /** Jar base name the class was loaded from, or null when it has no class source. */
    private static String jarNameOfCodeSource(Class<?> cls) {
        CodeSource cs = cls.getProtectionDomain().getCodeSource();
        if (cs == null || cs.getLocation() == null) return null;
        String jarName = jarNameOf(cs.getLocation());
        return jarName == null ? null : cleanJarName(jarName);
    }

    /**
     * Resolve the NeoForge mod id behind a class, mirroring spark:
     * {@code ModList.get().getModContainerByClass(cls).map(ModContainer::getModId)}.
     * Purely reflective, so MethodSampler never hard-depends on the FML loader classes.
     */
    private static String modIdOf(Class<?> cls) {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList == null) return null;
            Object result = modListClass.getMethod("getModContainerByClass", Class.class).invoke(modList, cls);
            if (result == null) return null;
            // Optional<ModContainer> — empty means the class is not a mod class
            Object container = result.getClass().getMethod("orElse", Object.class).invoke(result, (Object) null);
            if (container == null) return null;
            return (String) container.getClass().getMethod("getModId").invoke(container);
        } catch (Throwable t) {
            return null;
        }
    }

    // jar name ("goblintraders-neoforge-1.21.1-1.11.2") -> mod id ("goblintraders"),
    // built lazily from ModList.getMods() so every mod class maps to a clean mod id.
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
        // some mods ship a versioned jar name while the @Mod reference is unversioned
        String cleaned = cleanJarName(jarName);
        if (!cleaned.equals(jarName)) {
            id = jarNameToModId.get(cleaned);
        }
        return id;
    }

    /** Map every mod's jar base name to its mod id, via reflection (no hard FML dependency). */
    @SuppressWarnings("unchecked")
    private static Map<String, String> buildJarNameToModId() {
        Map<String, String> map = new HashMap<>();
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList == null) return map;
            Object mods = modListClass.getMethod("getMods").invoke(modList);
            if (!(mods instanceof List)) return map;
            // IModInfo -> getOwningFile() -> IModFileInfo -> getFile() -> IModFile
            //          -> getFilePath() -> Path -> getFileName()
            for (Object info : (List<Object>) mods) {
                String modId = (String) info.getClass().getMethod("getModId").invoke(info);
                Object fileInfo;
                try {
                    fileInfo = info.getClass().getMethod("getOwningFile").invoke(info);
                } catch (NoSuchMethodException e) {
                    fileInfo = info.getClass().getMethod("getModFileInfo").invoke(info);
                }
                if (fileInfo == null) continue;
                Object modFile = fileInfo.getClass().getMethod("getFile").invoke(fileInfo);
                if (modFile == null) continue;
                Object filePath = modFile.getClass().getMethod("getFilePath").invoke(modFile);
                if (filePath == null) continue;
                Object fileName = filePath.getClass().getMethod("getFileName").invoke(filePath);
                if (fileName == null) continue;
                String name = fileName.toString();
                if (name.endsWith(".jar")) name = name.substring(0, name.length() - 4);
                else if (name.endsWith(".zip")) name = name.substring(0, name.length() - 4);
                if (!name.isEmpty() && modId != null) {
                    map.put(name, modId);
                    // index the version-stripped name too, to match jarNameOfCodeSource()
                    String cleaned = cleanJarName(name);
                    if (!cleaned.equals(name)) map.put(cleaned, modId);
                }
            }
        } catch (Throwable ignored) {
            // an empty map just means we fall back to raw jar names
        }
        return map;
    }

    // jar base name ("essentialsx-2.20.0") -> plugin.yml name ("Essentials"), built lazily by
    // walking every loaded Bukkit plugin, so plugin classes resolve to the exact plugin name
    // even when their classloader cannot be introspected (Mohist hybrid).
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
     * Map each loaded plugin's jar base name to its plugin.yml name. The jar is located via the
     * plugin's own classloader, so it points at the plugin jar regardless of loader implementation.
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
            // no Bukkit: plugins fall back to MOD naming
        }
        return map;
    }

    /** Determine the jar base name a plugin was loaded from. */
    private static String pluginJarName(org.bukkit.plugin.Plugin plugin) {
        // prefer the plugin.yml resource URL from the plugin's own classloader
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

    /** Server loaders plus every plugin's classloader (plugin classes are invisible to the server loader). */
    private static ClassLoader[] allCandidateLoaders() {
        ClassLoader[] base = candidateClassLoaders();
        ClassLoader[] plugins = pluginLoaders();
        // the server context loader must come first: it is the one that can define mod classes
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

    /** Collect the classloaders of all loaded plugins, cached across calls. */
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

    /** Jar base name (without ".jar") from a class source location. */
    private static String jarNameOf(URL location) {
        try {
            String path = location.toURI().getPath();
            if (path == null) return null;
            // drop any "#..." fragment some loaders append
            int hash = path.indexOf('#');
            if (hash >= 0) path = path.substring(0, hash);
            // jar:file:/.../mod.jar!/com/x  -> keep only the path before "!/"
            int exclamation = path.indexOf("!/");
            if (exclamation >= 0) path = path.substring(0, exclamation);
            if (path.endsWith("!/")) path = path.substring(0, path.length() - 2);
            int slash = path.lastIndexOf('/');
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            if (name.endsWith(".jar")) {
                return name.substring(0, name.length() - 4);
            }
            // unpacked / directory source — use the directory name
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

    /** Strip version suffixes, e.g. "create-1.21.1-0.5.1" -> "create". */
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