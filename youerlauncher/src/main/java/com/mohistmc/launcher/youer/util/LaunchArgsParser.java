package com.mohistmc.launcher.youer.util;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;

/**
 * @author Mgazul
 * @date 2025/9/29 18:27
 */
public class LaunchArgsParser {

    public static final sun.misc.Unsafe unsafe;
    private static final MethodHandles.Lookup IMPL_LOOKUP;
    public static File universalJar;
    public static File PATCHED;
    public static File MC_SRG;
    public static File MC_EXTRA;

    static {
        try {
            Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
            MethodHandles.lookup().ensureInitialized(MethodHandles.Lookup.class);
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object base = unsafe.staticFieldBase(field);
            long offset = unsafe.staticFieldOffset(field);
            IMPL_LOOKUP = (MethodHandles.Lookup) unsafe.getObject(base, offset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private static void addExports(List<String> exports) {
        MethodHandle implAddExportsMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddExports", MethodType.methodType(void.class, String.class, Module.class));
        MethodHandle implAddExportsToAllUnnamedMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddExportsToAllUnnamed", MethodType.methodType(void.class, String.class));

        addExtra(exports, implAddExportsMH, implAddExportsToAllUnnamedMH);
    }

    @SneakyThrows
    private static void addOpens(List<String> opens) {
        MethodHandle implAddOpensMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddOpens", MethodType.methodType(void.class, String.class, Module.class));
        MethodHandle implAddOpensToAllUnnamedMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddOpensToAllUnnamed", MethodType.methodType(void.class, String.class));

        addExtra(opens, implAddOpensMH, implAddOpensToAllUnnamedMH);
    }

    private static ParserData parseModuleExtra(String extra) {
        String[] all = extra.split("=", 2);
        if (all.length < 2) {
            return null;
        }

        String[] source = all[0].split("/", 2);
        if (source.length < 2) {
            return null;
        }
        return new ParserData(source[0], source[1], all[1]);
    }

    private static void addExtra(List<String> extras, MethodHandle implAddExtraMH, MethodHandle implAddExtraToAllUnnamedMH) {
        extras.forEach(extra -> {
            ParserData data = parseModuleExtra(extra);
            if (data != null) {
                ModuleLayer.boot().findModule(data.module).ifPresent(m -> {
                    try {
                        if ("ALL-UNNAMED".equals(data.target)) {
                            implAddExtraToAllUnnamedMH.invokeWithArguments(m, data.packages);
                        } else {
                            ModuleLayer.boot().findModule(data.target).ifPresent(tm -> {
                                try {
                                    implAddExtraMH.invokeWithArguments(m, data.packages, tm);
                                } catch (Throwable t) {
                                    throw new RuntimeException(t);
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

    @SneakyThrows
    public static void init(List<String> args) {
        List<String> opens = new ArrayList<>();
        List<String> exports = new ArrayList<>();

        String classpath = null;

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith("-")) {
                if (arg.equals("-classpath")) {
                    if (i + 1 < args.size()) {
                        classpath = args.get(i + 1);
                        i++;
                    }
                } else if (arg.startsWith("--add-opens")) {
                    opens.add(arg.substring("--add-opens ".length()).trim());
                } else if (arg.startsWith("--add-exports")) {
                    exports.add(arg.substring("--add-exports ".length()).trim());
                } else if (arg.startsWith("-D")) {
                    var split = arg.substring(2).split("=", 2);
                    System.setProperty(split[0], split[1]);
                }
            }
        }

        if (classpath != null) {
            System.setProperty("java.class.path", classpath); // TODO Seems to be repeated
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

    private record ParserData(String module, String packages, String target) {
    }
}
