/*
 * Youer - MohistMC
 * Copyright (C) 2018-2026.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.launcher.youer.feature;

import com.mohistmc.launcher.youer.Main;
import com.mohistmc.launcher.youer.util.JarLoader;
import com.mohistmc.launcher.youer.util.JarModule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Detects jars that would claim the same JPMS module name or the same package in two
 * different module layers and takes the legacy classpath copy out of the module layer.
 * <p>
 * The bootstrap launcher turns every legacy classpath entry into a module of the
 * MC-BOOTSTRAP layer, while mods and the libraries they bundle become modules of the
 * plugin and game layers. The resolver of the JDK refuses such a setup when a module
 * reads two modules with the same name or when two of them offer the same package to
 * a module that reads both, the resolver then reports
 * {@code Module x reads more than one module named y} or
 * {@code Modules x and y export package z to module w}.
 * <p>
 * The check here follows the rules of the JDK resolver, a module reads the modules it
 * requires and an automatic module reads every module of its own layer and of every
 * layer below it, an automatic module offers all of its packages while an explicit one
 * only offers its exports.
 * <p>
 * The legacy copy is only removed from the module layer, it is appended to the system
 * class loader instead, so its classes stay reachable from the unnamed module and the
 * library the mod bundles is the one that gets used. A legacy copy another module still
 * has to read is left alone, taking it out would only replace one failure of the resolver
 * with another one.
 * <p>
 * The entry is dropped from {@code legacyClassPath} rather than added to the
 * {@code ignoreList} of the bootstrap launcher, because that list keeps the entry on the
 * classpath and a jar that is both a classpath entry of the system loader and an ignored
 * legacy entry makes the mod launcher lose the {@code minecraft} module of the game layer.
 */
public final class ModuleConflictResolver {

    private static final String JARJAR_PREFIX = "META-INF/jarjar/";
    private static final String LEGACY_CLASS_PATH = "legacyClassPath";

    private ModuleConflictResolver() {
    }

    public static void resolve(List<String> args, List<String> legacyClassPath) {
        List<JarModule> boot = legacyModules(legacyClassPath);
        List<JarModule> game = gameModules(modFiles(args));
        if (boot.isEmpty() || game.isEmpty()) {
            return;
        }

        Layer bootLayer = new Layer(boot, List.of());
        Layer pluginLayer = new Layer(pluginModules(args), List.of(bootLayer));
        Layer gameLayer = new Layer(game, List.of(pluginLayer, bootLayer));

        Set<JarModule> legacy = new LinkedHashSet<>(boot);
        Set<JarModule> conflicting = new LinkedHashSet<>();
        for (JarModule reader : game) {
            collect(reader, gameLayer, legacy, conflicting);
        }
        for (JarModule reader : pluginLayer.modules()) {
            collect(reader, pluginLayer, legacy, conflicting);
        }
        if (conflicting.isEmpty()) {
            return;
        }

        Set<Path> dropped = new LinkedHashSet<>();
        for (JarModule module : conflicting) {
            if (isRequired(module, bootLayer, pluginLayer, gameLayer)) {
                // the module stays, taking it out would leave the module that requires
                // it without it and the resolver of the JDK would fail as well
                if (Main.DEBUG) {
                    System.out.println("[ModuleConflict] module '" + module.name() + "' of " + module.source()
                        + " collides with a module of the mod layer and is required by another module,"
                        + " it stays a module of the bootstrap layer");
                }
                continue;
            }
            dropped.add(module.source());
            JarLoader.loadJar(module.source());
        }
        if (dropped.isEmpty()) {
            return;
        }
        List<String> fileNames = removeFromLegacyClassPath(legacyClassPath, dropped);
        if (Main.DEBUG) {
            for (String fileName : fileNames) {
                System.out.println("[ModuleConflict] " + fileName + " is no longer a module of the"
                    + " bootstrap layer, the copy the mods ship is used instead");
            }
        }
    }

    /**
     * Takes the given jars out of the legacy classpath the bootstrap launcher turns into
     * the modules of its layer and returns the file names of the entries that went away.
     */
    private static List<String> removeFromLegacyClassPath(List<String> legacyClassPath, Set<Path> dropped) {
        Set<Path> sources = new LinkedHashSet<>();
        for (Path path : dropped) {
            sources.add(path.toAbsolutePath().normalize());
        }
        List<String> kept = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        for (String entry : legacyClassPath) {
            if (sources.contains(Path.of(entry).toAbsolutePath().normalize())) {
                removed.add(Path.of(entry).getFileName().toString());
            } else {
                kept.add(entry);
            }
        }
        if (!removed.isEmpty()) {
            System.setProperty(LEGACY_CLASS_PATH, String.join(File.pathSeparator, kept));
        }
        return removed;
    }

    /**
     * Tells whether a module still has to read the given module, the resolver of the
     * JDK then fails with {@code Module x not found, required by y} when it is gone.
     */
    private static boolean isRequired(JarModule module, Layer bootLayer, Layer pluginLayer, Layer gameLayer) {
        for (Layer layer : List.of(bootLayer, pluginLayer, gameLayer)) {
            for (JarModule reader : layer.modules()) {
                if (reader == module) {
                    continue;
                }
                for (String required : reader.mandatoryRequires()) {
                    Located located = layer.locate(required);
                    if (located != null && located.module() == module) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Walks the modules the reader reads and reports the ones that offer the same
     * module name or the same package twice, exactly like the resolver of the JDK does.
     */
    private static void collect(JarModule reader, Layer layer, Set<JarModule> legacy, Set<JarModule> conflicting) {
        Map<String, JarModule> byName = new HashMap<>();
        byName.put(reader.name(), reader);
        Map<String, JarModule> byPackage = new HashMap<>();
        for (String pkg : reader.packages()) {
            byPackage.put(pkg, reader);
        }

        for (JarModule module : reads(reader, layer)) {
            JarModule sameName = byName.putIfAbsent(module.name(), module);
            if (sameName != null) {
                mark(sameName, module, legacy, conflicting);
            }
            for (String pkg : module.suppliedTo(reader.name())) {
                JarModule samePackage = byPackage.putIfAbsent(pkg, module);
                if (samePackage != null) {
                    mark(samePackage, module, legacy, conflicting);
                }
            }
        }
    }

    /**
     * The modules the reader reads, the ones it requires plus, when it is an automatic
     * module, every module of its own layer and of every layer below it.
     */
    private static Set<JarModule> reads(JarModule reader, Layer layer) {
        Set<JarModule> reads = new LinkedHashSet<>();
        for (String required : reader.requires()) {
            Located located = layer.locate(required);
            if (located == null) {
                continue;
            }
            reads.add(located.module());
            if (located.module().automatic()) {
                // a module that requires an automatic module of a lower layer reads
                // all the automatic modules that one reads
                for (JarModule other : located.layer().automaticModules()) {
                    if (other != located.module()) {
                        reads.add(other);
                    }
                }
            }
        }
        if (reader.automatic()) {
            for (JarModule module : layer.all()) {
                if (module != reader) {
                    reads.add(module);
                }
            }
        }
        return reads;
    }

    /**
     * Remembers the legacy jar of a conflict, only a conflict between a legacy jar and
     * a mod can be solved by taking the legacy jar out of the module layer.
     */
    private static void mark(JarModule first, JarModule second, Set<JarModule> legacy, Set<JarModule> conflicting) {
        boolean firstLegacy = legacy.contains(first);
        boolean secondLegacy = legacy.contains(second);
        if (firstLegacy && !secondLegacy) {
            conflicting.add(first);
        } else if (secondLegacy && !firstLegacy) {
            conflicting.add(second);
        }
    }

    private static List<JarModule> legacyModules(List<String> legacyClassPath) {
        List<JarModule> modules = new ArrayList<>();
        for (String entry : legacyClassPath) {
            Path path = Path.of(entry);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            JarModule module = JarModule.of(path);
            if (module != null) {
                modules.add(module);
            }
        }
        return modules;
    }

    private static List<JarModule> gameModules(Set<Path> modFiles) {
        List<JarModule> modules = new ArrayList<>();
        for (Path mod : modFiles) {
            JarModule module = JarModule.of(mod);
            if (module != null) {
                modules.add(module);
            }
            collectNestedModules(mod, modules);
        }
        return modules;
    }

    private static void collectNestedModules(Path mod, List<JarModule> modules) {
        try (ZipFile zip = new ZipFile(mod.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(JARJAR_PREFIX) || !name.endsWith(".jar")) {
                    continue;
                }
                JarModule module = JarModule.ofNested(zip, entry);
                if (module != null) {
                    modules.add(module);
                }
            }
        } catch (IOException e) {
            // an unreadable mod simply contributes no module
        }
    }

    private static Set<Path> modFiles(List<String> args) {
        Set<Path> files = new LinkedHashSet<>();
        File[] mods = new File("mods").listFiles((dir, name) -> name.endsWith(".jar"));
        if (mods != null) {
            for (File mod : mods) {
                files.add(mod.toPath());
            }
        }
        files.addAll(universalJars(args));
        files.addAll(layerLibraries(args, "fml.gameLayerLibraries"));
        return files;
    }

    private static List<JarModule> pluginModules(List<String> args) {
        List<JarModule> modules = new ArrayList<>();
        for (Path path : layerLibraries(args, "fml.pluginLayerLibraries")) {
            JarModule module = JarModule.of(path);
            if (module != null) {
                modules.add(module);
            }
            collectNestedModules(path, modules);
        }
        return modules;
    }

    private static List<Path> universalJars(List<String> args) {
        String libraryDirectory = property(args, "libraryDirectory", "libraries");
        String version = argumentValue(args, "--fml.neoForgeVersion ");
        if (version != null) {
            Path jar = Path.of(libraryDirectory, "net/neoforged/neoforge", version, "neoforge-" + version + "-universal.jar");
            if (Files.isRegularFile(jar)) {
                return List.of(jar);
            }
        }
        Path root = Path.of(libraryDirectory);
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root, 8)) {
            return stream.filter(path -> path.getFileName().toString().endsWith("-universal.jar"))
                .filter(Files::isRegularFile)
                .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static List<Path> layerLibraries(List<String> args, String key) {
        String libraryDirectory = property(args, "libraryDirectory", "libraries");
        List<Path> paths = new ArrayList<>();
        for (String name : property(args, key, "").split(",")) {
            if (name.isBlank()) {
                continue;
            }
            Path path = Path.of(name);
            if (!Files.isRegularFile(path)) {
                path = Path.of(libraryDirectory, name);
            }
            if (Files.isRegularFile(path)) {
                paths.add(path);
            }
        }
        return paths;
    }

    private static String argumentValue(List<String> args, String prefix) {
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                String value = arg.substring(prefix.length()).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String property(List<String> args, String key, String defaultValue) {
        String prefix = "-D" + key + "=";
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return defaultValue;
    }

    /**
     * A layer of modules, the layers below it are the ones a module of this layer reads
     * when it is an automatic module.
     */
    private static final class Layer {

        private final List<JarModule> modules;
        private final List<Layer> parents;

        private Layer(List<JarModule> modules, List<Layer> parents) {
            this.modules = modules;
            this.parents = parents;
        }

        private List<JarModule> modules() {
            return modules;
        }

        /**
         * Every module of this layer and of the layers below it.
         */
        private List<JarModule> all() {
            List<JarModule> all = new ArrayList<>(modules);
            for (Layer parent : parents) {
                all.addAll(parent.all());
            }
            return all;
        }

        private List<JarModule> automaticModules() {
            List<JarModule> automatic = new ArrayList<>();
            for (JarModule module : all()) {
                if (module.automatic()) {
                    automatic.add(module);
                }
            }
            return automatic;
        }

        /**
         * The module with the given name, the own layer comes first, then the layers
         * below it.
         */
        private Located locate(String name) {
            for (JarModule module : modules) {
                if (module.name().equals(name)) {
                    return new Located(module, this);
                }
            }
            for (Layer parent : parents) {
                Located located = parent.locate(name);
                if (located != null) {
                    return located;
                }
            }
            return null;
        }
    }

    private record Located(JarModule module, Layer layer) {
    }
}
