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

package com.mohistmc.launcher.youer.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * The JPMS module a jar becomes at runtime.
 * <p>
 * The bootstrap launcher turns every legacy classpath entry into a module of the
 * MC-BOOTSTRAP layer while mods and the libraries they bundle become modules of the
 * plugin and game layers. The resolver of the JDK refuses such a setup when two of
 * those modules claim the same name or when two of them offer the same package to
 * a module that reads both, so the very same view on a jar is needed here to tell
 * whether two jars would collide.
 */
public final class JarModule {

    private static final Pattern DASH_VERSION = Pattern.compile("-([.\\d]+)");
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern REPEATING_DOTS = Pattern.compile("(\\.(\\1))+");
    private static final Pattern LEADING_DOTS = Pattern.compile("^\\.");
    private static final Pattern TRAILING_DOTS = Pattern.compile("\\.$");
    private static final Pattern NUMBERLIKE_PARTS = Pattern.compile("(?<=^|\\.)([0-9]+)");
    private static final Pattern KEYWORD_PARTS = Pattern.compile("(?<=^|\\.)("
        + String.join("|", "abstract", "continue", "for", "new", "switch", "assert", "default", "goto",
        "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements",
        "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof",
        "return", "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface",
        "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float", "native",
        "super", "while")
        + ")(?=\\.|$)");

    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
    private static final String MODULE_INFO_PATH = "module-info.class";
    private static final String VERSIONS_PREFIX = "META-INF/versions/";
    private static final String MODULE_INFO_SUFFIX = "/" + MODULE_INFO_PATH;

    private final Path source;
    private final String name;
    private final boolean automatic;
    private final Set<String> packages;
    private final Map<String, Set<String>> exports;
    private final Set<String> requires;
    private final Set<String> staticRequires;

    private JarModule(Path source, String name, boolean automatic, Set<String> packages, Map<String, Set<String>> exports, Set<String> requires, Set<String> staticRequires) {
        this.source = source;
        this.name = name;
        this.automatic = automatic;
        this.packages = packages;
        this.exports = exports;
        this.requires = requires;
        this.staticRequires = staticRequires;
    }

    /**
     * Reads the module a jar on disk becomes.
     *
     * @param jar path of the jar, also used to derive the name from the file layout
     * @return the module, or null when the jar cannot be read
     */
    public static JarModule of(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Manifest manifest = readManifest(zip);
            byte[] descriptor = findModuleInfo(zip, manifest);
            Set<String> packages = new HashSet<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                addPackages(packages, entries.nextElement().getName());
            }
            return build(jar, descriptor, manifest, packages);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reads the module a jar nested inside another jar becomes.
     *
     * @param parent the jar holding the nested jar
     * @param entry  the entry of the nested jar
     * @return the module, or null when the nested jar cannot be read
     */
    public static JarModule ofNested(ZipFile parent, ZipEntry entry) {
        try (InputStream in = parent.getInputStream(entry); ZipInputStream nested = new ZipInputStream(in)) {
            Manifest manifest = null;
            byte[] descriptor = null;
            int descriptorVersion = -1;
            int runtime = Runtime.version().feature();
            Set<String> packages = new HashSet<>();
            ZipEntry current;
            while ((current = nested.getNextEntry()) != null) {
                String name = current.getName();
                if (MANIFEST_PATH.equals(name)) {
                    manifest = new Manifest(nested);
                } else if (MODULE_INFO_PATH.equals(name)) {
                    descriptor = nested.readAllBytes();
                } else if (name.startsWith(VERSIONS_PREFIX) && name.endsWith(MODULE_INFO_SUFFIX)) {
                    int version = parseVersion(name);
                    if (version <= runtime && version > descriptorVersion) {
                        descriptorVersion = version;
                        descriptor = nested.readAllBytes();
                    }
                } else {
                    addPackages(packages, name);
                }
                nested.closeEntry();
            }
            // the runtime sees a nested jar as the root of its own file system, so only the file name matters
            return build(Path.of(entry.getName()).getFileName(), descriptor, manifest, packages);
        } catch (IOException e) {
            return null;
        }
    }

    private static JarModule build(Path path, byte[] descriptor, Manifest manifest, Set<String> packages) {
        if (descriptor != null) {
            try (InputStream in = new ByteArrayInputStream(descriptor)) {
                ModuleDescriptor module = ModuleDescriptor.read(in);
                Map<String, Set<String>> exports = new LinkedHashMap<>();
                for (ModuleDescriptor.Exports export : module.exports()) {
                    exports.put(export.source(), export.targets());
                }
                Set<String> requires = new LinkedHashSet<>();
                Set<String> staticRequires = new LinkedHashSet<>();
                for (ModuleDescriptor.Requires require : module.requires()) {
                    requires.add(require.name());
                    if (require.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC)) {
                        staticRequires.add(require.name());
                    }
                }
                // securejarhandler adds every package of the jar to the descriptor it reads
                Set<String> modulePackages = new HashSet<>(module.packages());
                modulePackages.addAll(packages);
                return new JarModule(path, module.name(), false, modulePackages, exports, requires, staticRequires);
            } catch (InvalidModuleDescriptorException | IOException e) {
                // an unreadable descriptor leaves the jar an automatic module
            }
        }
        String automatic = manifest == null ? null : manifest.getMainAttributes().getValue("Automatic-Module-Name");
        Set<String> requires = Set.of("java.base");
        return new JarModule(path, automatic != null ? automatic : computeName(path), true, packages, Map.of(), requires, Set.of());
    }

    /**
     * Collects the packages of a jar the way securejarhandler does, a package is a
     * directory that holds a class file, the ones below META-INF are left out.
     */
    private static void addPackages(Set<String> packages, String entry) {
        if (!entry.endsWith(".class") || entry.startsWith("META-INF")) {
            return;
        }
        int index = entry.lastIndexOf('/');
        if (index <= 0) {
            return;
        }
        packages.add(entry.substring(0, index).replace('/', '.'));
    }

    private static Manifest readManifest(ZipFile zip) {
        ZipEntry entry = zip.getEntry(MANIFEST_PATH);
        if (entry == null) {
            return null;
        }
        try (InputStream in = zip.getInputStream(entry)) {
            return new Manifest(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] findModuleInfo(ZipFile zip, Manifest manifest) {
        ZipEntry entry = zip.getEntry(MODULE_INFO_PATH);
        if (entry != null) {
            try (InputStream in = zip.getInputStream(entry)) {
                return in.readAllBytes();
            } catch (IOException e) {
                return null;
            }
        }
        if (manifest == null || !"true".equalsIgnoreCase(manifest.getMainAttributes().getValue("Multi-Release"))) {
            return null;
        }
        int runtime = Runtime.version().feature();
        int best = -1;
        byte[] descriptor = null;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry candidate = entries.nextElement();
            String name = candidate.getName();
            if (!name.startsWith(VERSIONS_PREFIX) || !name.endsWith(MODULE_INFO_SUFFIX)) {
                continue;
            }
            int version = parseVersion(name);
            if (version <= runtime && version > best) {
                try (InputStream in = zip.getInputStream(candidate)) {
                    best = version;
                    descriptor = in.readAllBytes();
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return descriptor;
    }

    private static int parseVersion(String entryName) {
        try {
            return Integer.parseInt(entryName.substring(VERSIONS_PREFIX.length(), entryName.length() - MODULE_INFO_SUFFIX.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String computeName(Path path) {
        Path file = path.getFileName();
        if (file == null) {
            return null;
        }
        Path parent = path.getParent();
        if (parent != null && parent.getFileName() != null) {
            Path grandparent = parent.getParent();
            if (grandparent != null && grandparent.getFileName() != null) {
                String artifact = grandparent.getFileName().toString();
                if (file.toString().startsWith(artifact + "-" + parent.getFileName())) {
                    return cleanModuleName(artifact);
                }
            }
        }
        String fileName = file.toString();
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            fileName = fileName.substring(0, dot);
        }
        Matcher matcher = DASH_VERSION.matcher(fileName);
        if (matcher.find()) {
            return cleanModuleName(matcher.replaceAll(""));
        }
        return cleanModuleName(fileName);
    }

    private static String cleanModuleName(String name) {
        String cleaned = NON_ALPHANUM.matcher(name).replaceAll(".");
        cleaned = REPEATING_DOTS.matcher(cleaned).replaceAll(".");
        if (!cleaned.isEmpty() && cleaned.charAt(0) == '.') {
            cleaned = LEADING_DOTS.matcher(cleaned).replaceAll("");
        }
        if (!cleaned.isEmpty() && cleaned.charAt(cleaned.length() - 1) == '.') {
            cleaned = TRAILING_DOTS.matcher(cleaned).replaceAll("");
        }
        cleaned = NUMBERLIKE_PARTS.matcher(cleaned).replaceAll("_$1");
        cleaned = KEYWORD_PARTS.matcher(cleaned).replaceAll("_$1");
        return cleaned;
    }

    public Path source() {
        return source;
    }

    public String name() {
        return name;
    }

    public boolean automatic() {
        return automatic;
    }

    public Set<String> packages() {
        return packages;
    }

    public Set<String> requires() {
        return requires;
    }

    /**
     * The modules the module has to be able to read, the resolver of the JDK skips a
     * static require when the module it names is missing.
     */
    public Set<String> mandatoryRequires() {
        Set<String> mandatory = new LinkedHashSet<>(requires);
        mandatory.removeAll(staticRequires);
        return mandatory;
    }

    /**
     * The packages the module offers to the module named by the reader, an automatic
     * module offers all of its packages while an explicit one only offers its exports.
     */
    public Set<String> suppliedTo(String reader) {
        if (automatic) {
            return packages;
        }
        Set<String> supplied = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> export : exports.entrySet()) {
            Set<String> targets = export.getValue();
            if (targets.isEmpty() || targets.contains(reader)) {
                supplied.add(export.getKey());
            }
        }
        return supplied;
    }
}
