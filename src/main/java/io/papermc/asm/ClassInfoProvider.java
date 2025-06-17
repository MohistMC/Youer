package io.papermc.asm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Nullable;

@DefaultQualifier(NonNull.class)
@FunctionalInterface
public interface ClassInfoProvider {

    io.papermc.asm.ClassInfo info(String className);

    static ClassInfoProvider basic() {
        return className -> {
            try {
                // Note: Will not work if the class is not already on the classpath, most likely don't want to use this
                final Class<?> clazz = Class.forName(className.replace("/", "."));
                return io.papermc.asm.ClassInfo.create(
                    className,
                    clazz.isEnum(),
                    clazz.getSuperclass() == null ? null : clazz.getSuperclass().getName().replace(".", "/")
                );
            } catch (final ClassNotFoundException ex) {
                return null;
            }
        };
    }

    static ClassInfoProvider caching(
        final ClassInfoProvider backing,
        final boolean cacheMisses,
        final int cacheSize
    ) {
        return new ClassInfoProvider() {
            private static final io.papermc.asm.ClassInfo NULL_INFO = io.papermc.asm.ClassInfo.create("", false, null);

            private final Map<String, io.papermc.asm.ClassInfo> classInfoCache = Collections.synchronizedMap(new LinkedHashMap<>(cacheSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, io.papermc.asm.ClassInfo> eldest) {
                    return this.size() > cacheSize - 1;
                }
            });

            @Override
            public @Nullable io.papermc.asm.ClassInfo info(final String className) {
                final @Nullable io.papermc.asm.ClassInfo info = this.classInfoCache.computeIfAbsent(className, cls -> {
                    final @Nullable ClassInfo find = backing.info(cls);
                    return find == null && cacheMisses ? NULL_INFO : find;
                });
                return info == NULL_INFO ? null : info;
            }
        };
    }
}
