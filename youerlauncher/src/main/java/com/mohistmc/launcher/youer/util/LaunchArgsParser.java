package com.mohistmc.launcher.youer.util;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;

/**
 * @author Mgazul
 * @date 2025/9/29 18:27
 */
public class LaunchArgsParser {

    private static final String ADD_OPENS_PREFIX = "--add-opens ";
    private static final String ADD_EXPORTS_PREFIX = "--add-exports ";
    private static final String CLASSPATH_ARG = "-classpath";
    private static final String SYSTEM_PROPERTY_PREFIX = "-D";
    private static final String MODULE_SEPARATOR = "=";
    private static final String PACKAGE_SEPARATOR = "/";

    private static final MethodHandles.Lookup IMPL_LOOKUP;

    static {
        export(ModuleLayer.boot().findModule("java.base").orElseThrow(), "jdk.internal.module", LaunchArgsParser.class.getModule());
        IMPL_LOOKUP = MethodHandles.lookup();
    }

    @SneakyThrows
    private static void addExports(List<String> exports) {
        var modulesCl = IMPL_LOOKUP.findClass("jdk.internal.module.Modules");
        MethodHandle implAddExportsToAllUnnamedMH = IMPL_LOOKUP.findStatic(modulesCl, "addExportsToAllUnnamed", MethodType.methodType(void.class, Module.class, String.class));

        processModuleDirectives(exports, ModuleDirectiveType.EXPORT, implAddExportsToAllUnnamedMH);
    }

    @SneakyThrows
    private static void addOpens(List<String> opens) {
        var modulesCl = IMPL_LOOKUP.findClass("jdk.internal.module.Modules");
        MethodHandle implAddOpensToAllUnnamedMH = IMPL_LOOKUP.findStatic(modulesCl, "addOpensToAllUnnamed", MethodType.methodType(void.class, Module.class, String.class));

        processModuleDirectives(opens, ModuleDirectiveType.OPEN, implAddOpensToAllUnnamedMH);
    }

    private static ModuleDirectiveData parseModuleDirective(String directive) {
        String[] all = directive.split(MODULE_SEPARATOR, 2);
        if (all.length < 2) {
            return null;
        }

        String[] source = all[0].split(PACKAGE_SEPARATOR, 2);
        if (source.length < 2) {
            return null;
        }
        return new ModuleDirectiveData(source[0], source[1], all[1]);
    }

    private static void processModuleDirectives(List<String> directives, ModuleDirectiveType directiveType, MethodHandle implAddExtraToAllUnnamedMH) {
        directives.forEach(directive -> {
            ModuleDirectiveData data = parseModuleDirective(directive);
            if (data != null) {
                ModuleLayer.boot().findModule(data.module).ifPresent(m -> {
                    try {
                        if ("ALL-UNNAMED".equals(data.target)) {
                            implAddExtraToAllUnnamedMH.invokeWithArguments(m, data.packages);
                        } else {
                            ModuleLayer.boot().findModule(data.target).ifPresent(to -> {
                                if (directiveType == ModuleDirectiveType.OPEN) {
                                    open(m, data.packages, to);
                                } else if (directiveType == ModuleDirectiveType.EXPORT) {
                                    export(m, data.packages, to);
                                }
                            });
                        }
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
            }
        });
    }

    public enum ModuleDirectiveType {
        OPEN,
        EXPORT
    }

    private static void export(Module module, String pkg, Module to) {
        JarLoader.inst.redefineModule(module, Set.of(), Map.of(pkg, Set.of(to)), Map.of(), Set.of(), Map.of());
    }

    private static void open(Module module, String pkg, Module to) {
        JarLoader.inst.redefineModule(module, Set.of(), Map.of(), Map.of(pkg, Set.of(to)), Set.of(), Map.of());
    }

    @SneakyThrows
    public static void init(List<String> args) {
        List<String> opens = new ArrayList<>();
        List<String> exports = new ArrayList<>();

        String classpath = null;

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith("-")) {
                if (arg.equals(CLASSPATH_ARG)) {
                    if (i + 1 < args.size()) {
                        classpath = args.get(i + 1);
                        i++;
                    }
                } else if (arg.startsWith(ADD_OPENS_PREFIX)) {
                    opens.add(arg.substring(ADD_OPENS_PREFIX.length()).trim());
                } else if (arg.startsWith(ADD_EXPORTS_PREFIX)) {
                    exports.add(arg.substring(ADD_EXPORTS_PREFIX.length()).trim());
                } else if (arg.startsWith(SYSTEM_PROPERTY_PREFIX)) {
                    var split = arg.substring(2).split("=", 2);
                    System.setProperty(split[0], split.length > 1 ? split[1] : "");
                }
            }
        }

        if (classpath != null) {
            System.setProperty("java.class.path", classpath);
            loadModules(classpath);
        }

        addOpens(opens);
        addExports(exports);
    }

    public static void loadModules(String modulePath) {
        Arrays.stream(modulePath.split(File.pathSeparator))
                .map(Paths::get)
                .forEach(JarLoader::loadJar);
    }

    private record ModuleDirectiveData(String module, String packages, String target) {
    }
}
