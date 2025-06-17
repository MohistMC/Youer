package io.papermc.asm;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface ClassInfo {
    String name();

    boolean isEnum();

    @Nullable String superClassName();

    static ClassInfo create(
        final String name,
        final boolean isEnum,
        final @Nullable String superClassName
    ) {
        return new ClassInfoImpl(name, isEnum, superClassName);
    }
}
