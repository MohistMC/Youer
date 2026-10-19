package com.mohistmc.youer.bukkit.remapping;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The mojang -&gt; spigot member table that the server ships for plugin remapping
 * ({@code META-INF/mappings/reobf.tiny}). It is generated for the exact Minecraft
 * version the server was installed with.
 *
 * <p>The running server names its members the way this table's source column does, and a
 * Paper plugin reads those names back from reflection. What a plugin expects for a given
 * member, though, is whatever its own translation of this table produces for it, and that
 * translation starts by looking the class up under the name the plugin can see - a plugin
 * that cannot find the class there leaves the member name alone. So the names
 * {@link ReflectionHandler} shows a plugin depend on how the two namespaces spell the same
 * class, which {@link #mapClassName(String)} answers next to the name the plugin holds, and a
 * method has one more requirement: the plugin keys it by its parameters and reads that key in
 * the destination namespace, so a row whose parameters either namespace renames is left out of
 * {@link #methodName(String, String, String)} - the plugin would miss it and search the name it
 * started from, which is the name the running server uses.
 *
 * <p>A plugin compiled against Spigot still asks for the short names the spigot distribution
 * really does run with, which is the other direction of the same table and what
 * {@link #runtimeMethodName} and {@link #runtimeFieldName} answer.
 *
 * <p>{@code mappings/spigot2srg.srg} carries the same information but lags behind on classes
 * whose names changed between Minecraft versions ({@code NBTBase -> Tag}), which makes
 * SpecialSource miss those members and hand plugins the runtime name instead.
 *
 * @author Mgazul
 */
public final class ReobfTiny {

    public static final ReobfTiny INSTANCE = load();

    private static final Logger LOGGER = LogManager.getLogger("Youer");
    private static final String RESOURCE = "META-INF/mappings/reobf.tiny";

    // mojmap class -> spigot class, both internal names
    private final Map<String, String> classes = new HashMap<>();
    // spigot class -> mojmap class, only where the two namespaces spell it differently
    private final Map<String, String> runtimeClasses = new HashMap<>();
    // mojmap owner + "/" + mojmap member name + mojmap parameter descriptor -> spigot member name,
    // only the rows a plugin can reach, see resolveMethods()
    private final Map<String, String> methodNames = new HashMap<>();
    // renamed method rows, kept until the class table is complete enough to judge their parameters
    private final List<MethodRow> renamedMethods = new ArrayList<>();
    // spigot owner + "/" + spigot member name + mojmap descriptor -> mojmap member name
    private final Map<String, String> runtimeMethods = new HashMap<>();
    // mojmap owner + "/" + field name -> spigot field name
    private final Map<String, String> fieldNames = new HashMap<>();
    // spigot owner + "/" + spigot field name -> mojmap field name
    private final Map<String, String> runtimeFields = new HashMap<>();
    // member keys the table could not resolve to a single mojmap name
    private final Set<String> ambiguousFields = new HashSet<>();
    // plugin class names the table could not resolve to a single runtime class
    private int conflicts;
    // renamed method rows whose parameters are not spelled alike in both namespaces
    private int unkeyableMethods;

    private record MethodRow(String owner, String source, String destination, String params) {
    }

    /**
     * Whether the table describes this class at all. A member of a class it does not
     * know (a patch or mod addition) is untranslated for a plugin as well.
     */
    public boolean knows(String owner) {
        return classes.containsKey(owner);
    }

    /**
     * The class name a plugin presents for this runtime class.
     */
    public String mapClassName(String owner) {
        return classes.get(owner);
    }

    /**
     * The name a plugin holds for this runtime method, or null when the table has no row a
     * plugin can reach for it. Methods are keyed by their parameters because the two
     * namespaces overload them.
     *
     * @param desc the descriptor of the running member, which is already in this table's
     *             source namespace.
     */
    public String methodName(String owner, String name, String desc) {
        return methodNames.get(owner + "/" + name + paramsDesc(desc));
    }

    /**
     * The runtime name of a member a plugin asks for under its spigot name.
     *
     * @param paramsDesc the parameter descriptor built from the classes the plugin passed,
     *                   which are runtime classes, so it is already in this table's source
     *                   namespace.
     */
    public String runtimeMethodName(String owner, String pluginName, String paramsDesc) {
        return runtimeMethods.get(owner + "/" + pluginName + paramsDesc);
    }

    /**
     * The name a plugin holds for this runtime field.
     */
    public String fieldName(String owner, String name) {
        return fieldNames.get(owner + "/" + name);
    }

    public String runtimeFieldName(String owner, String pluginName) {
        return runtimeFields.get(owner + "/" + pluginName);
    }

    /**
     * The runtime field a plugin asks for, accepting the casing and legacy names plugins use.
     */
    public String runtimeFieldName(Class<?> clazz, String pluginName) {
        if (clazz == null || !clazz.getName().startsWith("net.minecraft.")) return null;
        try {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(pluginName)) return pluginName;
            }
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(pluginName)) return field.getName();
            }
            if ("CRAFTER".equals(pluginName)) {
                for (Field field : clazz.getDeclaredFields()) {
                    if ("CRAFTER_3x3".equals(field.getName())) return field.getName();
                }
            }
        } catch (Throwable ignored) {
        }
        return runtimeFieldName(clazz.getName().replace('.', '/'), pluginName);
    }

    /**
     * The runtime class a plugin names by its spigot name.
     */
    public String runtimeClassName(String pluginClassName) {
        if (pluginClassName == null || !pluginClassName.startsWith("net.minecraft.")) return null;
        String runtime = runtimeClassOf(pluginClassName.replace('.', '/'));
        return runtime == null ? null : runtime.replace('/', '.');
    }

    /**
     * The runtime class a plugin names this way.
     *
     * <p>The spigot namespace reuses names that the runtime gives to a different class:
     * it calls the fluid state {@code Fluid}, while the runtime {@code Fluid} is the fluid
     * type. A plugin that resolves such a name reflectively must not be answered with
     * whichever class happens to carry that name at runtime.
     */
    public String runtimeClassOf(String pluginClassName) {
        return runtimeClasses.get(pluginClassName);
    }

    private static ReobfTiny load() {
        ReobfTiny tiny = new ReobfTiny();
        // Nothing here may reach the caller as an error: a failed initialisation
        // would make this class permanently unusable for every plugin remapped
        // afterwards, while an empty table only restores the previous behaviour.
        try (InputStream stream = open()) {
            if (stream == null) {
                info("ReobfTiny: " + RESOURCE + " not found, only the bundled member names are known");
                return tiny;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String header = reader.readLine();
            String[] columns = header == null ? new String[0] : header.split("\t", -1);
            if (columns.length < 4 || !columns[3].contains("mojang")) {
                info("ReobfTiny: unexpected table header [" + header + "], ignored");
                return tiny;
            }
            String owner = null;
            String line;
            while ((line = reader.readLine()) != null) {
                owner = readLine(tiny, line, owner);
            }
            tiny.resolveMethods();
            info("ReobfTiny: loaded " + tiny.fieldNames.size() + " field names and " + tiny.methodNames.size() + " method names (" + tiny.unkeyableMethods + " rows no plugin can key) of " + tiny.classes.size() + " classes, " + tiny.runtimeClasses.size() + " class names differ from the runtime (" + tiny.conflicts + " ambiguous, " + tiny.ambiguousFields.size() + " ambiguous fields)");
        } catch (Throwable e) {
            warn("ReobfTiny: failed to read " + RESOURCE, e);
        }
        return tiny;
    }

    private static void info(String message) {
        try {
            LOGGER.info(message);
        } catch (Throwable e) {
            System.out.println(message);
        }
    }

    private static void warn(String message, Throwable error) {
        try {
            LOGGER.warn(message, error);
        } catch (Throwable e) {
            System.err.println(message);
            error.printStackTrace();
        }
    }

    /**
     * tiny v2 layout: an unindented {@code c} line names a class (source then
     * destination), and its members follow as tab-indented lines, so splitting a
     * member line leaves the entry type at index 1. Descriptors are written in the
     * source namespace.
     */
    private static String readLine(ReobfTiny tiny, String line, String owner) {
        boolean member = line.startsWith("\t");
        String[] parts = line.split("\t");
        int base = member ? 1 : 0;
        String kind = parts.length > base ? parts[base] : "";
        if (kind.equals("c")) {
            if (parts.length < 3) {
                return owner;
            }
            String source = parts[1];
            String destination = parts[2];
            tiny.classes.put(source, destination);
            if (!source.equals(destination)) {
                String previous = tiny.runtimeClasses.get(destination);
                if (previous == null) {
                    tiny.runtimeClasses.put(destination, source);
                } else if (!previous.equals(source)) {
                    // Two runtime classes answer to this plugin name, so the name alone
                    // cannot say which one was meant; leave it to the runtime.
                    tiny.runtimeClasses.remove(destination);
                    tiny.conflicts++;
                }
            }
            return source;
        }
        if (owner == null || parts.length < base + 4) {
            return owner;
        }
        if (kind.equals("f")) {
            String source = parts[base + 2];
            String destination = parts[base + 3];
            tiny.fieldNames.put(owner + "/" + source, destination);
            if (!source.equals(destination)) {
                String key = owner + "/" + destination;
                if (!tiny.ambiguousFields.contains(key)) {
                    String previous = tiny.runtimeFields.putIfAbsent(key, source);
                    if (previous != null && !previous.equals(source)) {
                        // Two runtime fields answer to this plugin name, so the name alone
                        // cannot say which one was meant.
                        tiny.runtimeFields.remove(key);
                        tiny.ambiguousFields.add(key);
                    }
                }
            }
        } else if (kind.equals("m")) {
            String source = parts[base + 2];
            String destination = parts[base + 3];
            if (source.equals(destination)) {
                return owner;
            }
            String params = paramsDesc(parts[base + 1]);
            tiny.renamedMethods.add(new MethodRow(owner, source, destination, params));
            tiny.runtimeMethods.putIfAbsent(owner + "/" + destination + params, source);
        }
        return owner;
    }

    /**
     * Keep the method rows a plugin can actually reach. A plugin keys a method by its
     * parameters as the destination namespace spells them and searches that key with the
     * descriptors of the classes it holds, which are the running ones, so a row naming a class
     * the two namespaces differ on is a row that plugin never finds - and it keeps searching
     * under the runtime name, which is what {@link ReflectionHandler} has to answer with.
     */
    private void resolveMethods() {
        for (MethodRow row : renamedMethods) {
            if (namesParamsLikeRuntime(row.params())) {
                methodNames.put(row.owner() + "/" + row.source() + row.params(), row.destination());
            } else {
                unkeyableMethods++;
            }
        }
        renamedMethods.clear();
    }

    private boolean namesParamsLikeRuntime(String params) {
        for (int index = 0; ; ) {
            int start = params.indexOf('L', index);
            if (start < 0) {
                return true;
            }
            int end = params.indexOf(';', start);
            if (end < 0) {
                return false;
            }
            String name = params.substring(start + 1, end);
            String destination = classes.get(name);
            if (destination != null && !destination.equals(name)) {
                return false;
            }
            index = end + 1;
        }
    }

    private static String paramsDesc(String desc) {
        int end = desc.indexOf(')');
        return desc.substring(1, end < 0 ? desc.length() : end);
    }

    private static InputStream open() {
        for (ClassLoader loader : new ClassLoader[]{ReobfTiny.class.getClassLoader(), ClassLoader.getSystemClassLoader()}) {
            if (loader == null) {
                continue;
            }
            try {
                InputStream stream = loader.getResourceAsStream(RESOURCE);
                if (stream != null) {
                    info("ReobfTiny: reading " + RESOURCE + " from " + loader);
                    return stream;
                }
            } catch (Throwable e) {
                warn("ReobfTiny: " + loader + " failed to look up " + RESOURCE, e);
            }
        }
        for (String name : new String[]{"paper-remap.jar", "paper-remap-0.jar"}) {
            Path path = Path.of("libraries", "com", "mohistmc", "installation", "data", name);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try (ZipFile zip = new ZipFile(path.toFile())) {
                ZipEntry entry = zip.getEntry(RESOURCE);
                if (entry == null) {
                    continue;
                }
                try (InputStream stream = zip.getInputStream(entry)) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    stream.transferTo(bytes);
                    info("ReobfTiny: reading " + path);
                    return new ByteArrayInputStream(bytes.toByteArray());
                }
            } catch (Throwable e) {
                warn("ReobfTiny: failed to read " + path, e);
            }
        }
        return null;
    }
}
