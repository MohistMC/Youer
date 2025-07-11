package io.papermc.asm;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
record ClassInfoImpl(
    String name,
    boolean isEnum,
    @Nullable String superClassName
) implements ClassInfo {
}
